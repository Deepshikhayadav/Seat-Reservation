```markdown
# Seat Reservation at Scale — Technical Write-up

## 1. Problem

The system provides a backend API for reserving seats for a show.

The most important correctness requirement is that concurrent users must not be able to reserve the same seat more than once.

The system also needs:

- JWT-based user identity
- per-user reservation limits
- idempotent reservation requests
- all-or-nothing multi-seat reservations
- cancellation
- health and readiness checks
- Prometheus metrics
- structured request logging
- Docker deployment
- concurrency testing against a public deployment

---

## 2. Architecture

The application uses a simple architecture:

```text
Client
  |
  | HTTPS / JSON
  v
Spring Boot API
  |
  +---- Spring Security JWT
  |
  +---- Reservation Service
  |
  +---- Metrics / Logging
  |
  v
PostgreSQL
```

PostgreSQL is the source of truth for reservation state.


---

## 3. Concurrency Control

### Problem

Consider two requests arriving at approximately the same time:

```text
User A -> reserve A1
User B -> reserve A1
```

Without concurrency control, both requests could read:

```text
A1 = AVAILABLE
```

and both could attempt to reserve it.

That would result in a double-sell.

### Solution

The application locks the requested seat rows using a pessimistic write lock.

The reservation flow is:

```text
1. Normalize and sort requested seats
2. Start database transaction
3. Lock requested seat rows
4. Check that all seats exist
5. Check that all seats are AVAILABLE
6. Create reservation
7. Create reservation_seats records
8. Mark seats CONFIRMED
9. Update per-user reservation count
10. Commit transaction
```

The important operation is the row lock.

While one transaction holds the lock for A1, another transaction attempting to reserve A1 must wait.

After the first transaction commits, the second transaction sees that A1 is no longer available and returns `409 Conflict`.

Therefore:

```text
50 concurrent requests
        |
        v
     A1 lock
        |
        +----> 1 request -> 201
        |
        +----> 49 requests -> 409
```

---

## 4. Deterministic Lock Ordering

Multi-seat requests can introduce another concurrency problem.

For example:

```text
Request A: A1, A2
Request B: A2, A1
```

If both transactions lock seats in different orders, they can create unnecessary lock contention or deadlock risk.

The application therefore normalizes the request:

```text
distinct()
sorted()
```

before acquiring seat locks.

Both requests therefore attempt to lock:

```text
A1
A2
```

in the same order.

This makes concurrent multi-seat reservations safer and more predictable.

---

## 5. All-or-Nothing Multi-seat Reservations

A reservation containing multiple seats is handled inside one database transaction.

Example:

```json
{
  "seats": ["A1", "A2"]
}
```

If both seats are available:

```text
A1 -> CONFIRMED
A2 -> CONFIRMED
Reservation -> CONFIRMED
```

If either seat is unavailable:

```text
Reservation -> rejected
A1 -> unchanged
A2 -> unchanged
```

No partial reservation is committed.

This is achieved by performing the availability checks and all writes inside the same transaction.

---

## 6. Per-user Limit

The default per-user limit is four seats per show.

A separate `user_show_limits` row stores:

```text
show_id
user_id
reserved_count
```

The row is locked during the reservation transaction.

For example:

```text
Current count = 3
Requested seats = 2
Limit = 4
```

The request is rejected because:

```text
3 + 2 > 4
```

Locking the user limit row is important because two concurrent requests from the same user could otherwise both observe the same old count.

---

## 7. Idempotency

Every reservation requires an `Idempotency-Key`.

The database stores:

```text
user_id
show_id
idempotency_key
request_hash
reservation_id
status
```

There is a unique constraint on:

```text
(user_id, show_id, idempotency_key)
```

The first request creates the idempotency record.

A retry with the same key and the same request returns the original reservation.

A request using the same key but a different seat list is rejected:

```text
409 Conflict
IDEMPOTENCY_CONFLICT
```

This prevents clients from accidentally creating duplicate reservations when retrying a request.

---

## 8. Database Constraints

Database constraints provide an additional layer of protection.

Important constraints include:

```text
UNIQUE(user_id, show_id, idempotency_key)
UNIQUE(seat_id)
UNIQUE(reservation_id, seat_id)
```

The unique `seat_id` constraint on `reservation_seats` is defense in depth.

Even if application-level logic were to fail, PostgreSQL would prevent the same seat from being associated with multiple reservations.

---

## 9. Cancellation

The API provides:

```text
POST /reservations/{reservationId}/cancel
```

Cancellation is transactional.

The application:

1. Locks the reservation.
2. Verifies that the authenticated user owns it.
3. Locks the reservation-seat records.
4. Locks the associated seat rows.
5. Changes the seats back to `AVAILABLE`.
6. Decreases the user's reserved count.
7. Marks the reservation as `CANCELLED`.
8. Commits the transaction.

This allows a cancelled seat to become available for another reservation.

---

## 10. Money Handling

Ticket prices are represented using integer paise.

For example:

```text
₹250.00 = 25000 paise
```

The database therefore stores:

```text
price_paise BIGINT
amount_paise BIGINT
```

This avoids floating-point precision problems when calculating monetary values.

---

## 11. Authentication

The reservation endpoint uses JWT authentication.

The user's identity comes from the JWT `sub` claim.

For example:

```json
{
  "sub": "user-1"
}
```

The application uses:

```java
authentication.getName()
```

as the user ID.

The JWT signing secret is supplied through configuration rather than hard-coded into the Spring application configuration used for deployment.

---

## 12. Observability

The application exposes health checks through Spring Boot Actuator.

### Liveness

```text
GET /actuator/health/liveness
```

### Readiness

```text
GET /actuator/health/readiness
```

Readiness includes database health, allowing the deployment platform to distinguish between a running process and an application that is actually ready to serve requests.

### Metrics

Prometheus metrics are available through:

```text
GET /actuator/prometheus
```

The application tracks:

```text
seat_reservation_confirmed_total
seat_reservation_declined_total{reason="seat_taken"}
seat_reservation_declined_total{reason="per_user_limit"}
seat_reservation_declined_total{reason="idempotent_replay"}
seat_reservation_available_seats
```

These metrics provide visibility into reservation success, contention, user-limit failures, retries, and seat availability.

---

## 13. Structured Logging

Each HTTP request receives a correlation ID.

Clients can provide:

```text
X-Request-ID
```

If it is not supplied, the application generates one.

The request ID is stored in the logging MDC and emitted as part of structured JSON logs.

This allows requests to be traced through application logs during debugging and production incidents.

---

## 14. Failure Handling

Expected reservation conflicts return:

```text
409 Conflict
```

Examples include:

```text
SEAT_TAKEN
PER_USER_LIMIT
IDEMPOTENCY_CONFLICT
```

Invalid requests return:

```text
400 Bad Request
```

Authorization failures return:

```text
403 Forbidden
```

Unexpected server failures return:

```text
500 Internal Server Error
```

The concurrency requirement specifically verifies that contention on the same seat produces `409` responses rather than `500` errors.

---

## 15. Testing Strategy

The most important test is the hot-seat concurrency test.

The test sends 50 simultaneous requests for the same seat:

```text
A1
```

Each request has a different idempotency key so that the requests represent independent reservation attempts.

Expected result:

```text
201 Created = 1
409 Conflict = 49
500 Error = 0
```

This demonstrates that exactly one request can reserve the seat.

A separate burst test sends multiple concurrent requests against the deployed API to exercise the service under a larger burst of traffic.

---

## 16. Deployment

The application is containerized using Docker.

The production deployment consists of:

```text
Render Web Service
        |
        v
Spring Boot Docker container
        |
        v
Render PostgreSQL
```

Database configuration is provided using environment variables.

The JDBC URL must use:

```text
jdbc:postgresql://...
```

Flyway runs database migrations during application startup.

---

## 17. Trade-offs

### PostgreSQL locking instead of Redis locking

PostgreSQL was chosen because the seat state and reservation records already live in the database.

Using row-level database locks keeps correctness close to the data and avoids introducing another distributed coordination system.

### Single application

The solution uses a single Spring Boot service.

This is sufficient for the assignment and keeps deployment and debugging simple.

### Pessimistic locking

Pessimistic locking was chosen because the primary requirement is correctness under contention for a small number of hot seat rows.

The trade-off is that highly contended seats can cause transactions to wait.

strong correctness is more important than optimizing a hypothetical very-high-scale workload.

---

## 18. Concurrency Test Result

The final public concurrency test should demonstrate:

```text
50 concurrent requests for A1

201: 1
409: 49
500: 0
```

This verifies the primary correctness property:

> A seat can be successfully reserved by exactly one concurrent request.

The final test result should be recorded here after testing the deployed application.

---

## 19. Conclusion

The implementation uses PostgreSQL transactions, pessimistic row locking, unique constraints, idempotency records, and per-user locking to provide correct reservation behavior under concurrency.

The system is intentionally small and operationally simple while still addressing the main production concerns required:

- correctness
- concurrency
- idempotency
- authorization
- observability
- health checks
- deployment
- testing
