```markdown
# Seat Reservation at Scale

A concurrent seat reservation API built with Spring Boot and PostgreSQL.

The system is designed to prevent double-selling under concurrent requests while supporting idempotency, per-user reservation limits, cancellation, health checks, metrics, structured logging, and Docker deployment.

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security JWT
- Micrometer / Prometheus
- Docker
- Docker Compose
- Render
- Python for API and concurrency testing

## Architecture

The application uses a single Spring Boot service backed by PostgreSQL.

PostgreSQL is responsible for the transactional correctness of reservations.

For concurrent reservations:

1. The requested seats are normalized and sorted.
2. Seat rows are locked using pessimistic database locking.
3. The application verifies that all requested seats are available.
4. The reservation and reservation-seat records are created in the same transaction.
5. The seats are marked as confirmed.
6. The user's reservation count is updated.
7. The idempotency record is marked as confirmed.

Because the seat rows are locked inside the transaction, concurrent requests cannot both successfully reserve the same seat.

## API

### Create a show

```http
POST /shows
```

Example request:

```json
{
  "name": "Avengers Movie",
  "seats": ["A1", "A2", "A3", "A4"],
  "price_paise": 25000
}
```

`price_paise` is stored as an integer to avoid floating-point money calculations.

### Get show

```http
GET /shows/{showId}
```

Returns seat availability and reservation counts.

### Reserve seats

```http
POST /shows/{showId}/reserve
```

Headers:

```text
Authorization: Bearer <JWT>
Idempotency-Key: <unique-key>
```

Example:

```json
{
  "seats": ["A1"]
}
```

Successful response:

```text
201 Created
```

### Cancel reservation

```http
POST /reservations/{reservationId}/cancel
```

The authenticated user can cancel their own confirmed reservation.

The cancelled seats become available again.

## Concurrency Safety

The reservation flow uses PostgreSQL row-level pessimistic locks.

Example:

```text
Request 1 ──┐
Request 2 ──┤
Request 3 ──┼──> PostgreSQL seat row lock
Request 4 ──┤
Request 5 ──┘

              ↓

        One request gets A1

              ↓

        A1 = CONFIRMED

              ↓

      Other requests receive 409
```

The `reservation_seats` table also has a unique constraint on `seat_id` as defense in depth against double-selling.

## Multi-seat Reservations

Multi-seat reservations are all-or-nothing.

For example:

```json
{
  "seats": ["A1", "A2"]
}
```

If both seats are available, both are reserved.

If either seat is already taken, the entire request is rejected with `409 Conflict`.

No partial reservation is created.

## Per-User Limit

The default per-user limit is 4 seats per show.

The current reservation count is stored in `user_show_limits`.

The user limit row is locked during reservation so concurrent requests from the same user cannot bypass the limit.

## Idempotency

Every reservation requires an `Idempotency-Key`.

The key is stored together with:

- user ID
- show ID
- request hash
- reservation ID
- processing status

Sending the same key with the same request returns the original reservation.

Sending the same key with a different request returns:

```text
409 Conflict
```

This prevents accidental duplicate reservations caused by client retries.

## Database

Flyway manages database schema migrations.

Main tables:

- `shows`
- `seats`
- `reservations`
- `reservation_seats`
- `idempotency_keys`
- `user_show_limits`

Important database constraints include:

```text
UNIQUE(user_id, show_id, idempotency_key)
UNIQUE(seat_id)
UNIQUE(reservation_id, seat_id)
```

## Authentication

The reservation endpoint uses JWT authentication.

The authenticated user's JWT `sub` claim is used as the application user ID.

For local testing, JWTs can be generated using:

```bash
python jwt_token.py user-1
```

The JWT signing secret must match the application's `JWT_SECRET`.

## Health Checks

Liveness:

```text
GET /actuator/health/liveness
```

Readiness:

```text
GET /actuator/health/readiness
```

The application also exposes:

```text
GET /actuator/health
```

Database health is included in Spring Boot's health checks.

## Metrics

Prometheus metrics are exposed at:

```text
GET /actuator/prometheus
```

Important metrics include:

```text
seat_reservation_confirmed_total
seat_reservation_declined_total{reason="seat_taken"}
seat_reservation_declined_total{reason="per_user_limit"}
seat_reservation_declined_total{reason="idempotent_replay"}
seat_reservation_available_seats
```

These metrics provide visibility into successful reservations, declined requests, idempotent replays, and current seat availability.

## Structured Logging

Requests include a correlation/request ID.

Clients can provide:

```text
X-Request-ID
```

If no request ID is provided, the application generates one.

Logs are emitted as structured JSON using Logstash Logback Encoder.

## Running Locally

Start PostgreSQL and the application:

```bash
docker compose up --build
```

The application runs on:

```text
http://localhost:8080
```

Check health:

```bash
curl http://localhost:8080/actuator/health
```

## Testing

### JWT

Generate a token:

```bash
python jwt_token.py user-1
```

### Hot-seat concurrency test

The concurrency test sends 50 simultaneous requests for the same seat.

```bash
python concurrency_test.py SHOW_ID
```

Expected result:

```text
201: 1
409: 49
500: 0
```

This verifies that exactly one request can reserve the same seat.

### Burst test

Run:

```bash
python burst_test.py SHOW_ID
```

The burst test sends multiple concurrent requests to the deployed application and reports the resulting HTTP status codes.

## Docker

Build:

```bash
docker compose build
```

Run:

```bash
docker compose up
```

Stop:

```bash
docker compose down
```

## Deployment

The application can be deployed as a Docker Web Service with PostgreSQL on Render.

Required environment variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
PORT
```

`PORT` is set to `8080` for local Docker execution.

For deployment, the database URL must use the PostgreSQL JDBC format:

```text
jdbc:postgresql://<host>:<port>/<database>
```

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/deepshikhayadav/seat_reservation/show/
│   └── resources/
│       ├── db/migration/
│       ├── application.yml
│       └── logback-spring.xml

Dockerfile
docker-compose.yml
burst_test.py
concurrency_test.py
jwt_token.py
README.md
WRITEUP.md
```

## Design Trade-offs

The implementation intentionally uses PostgreSQL as the source of truth for reservation correctness.

No Redis, Kafka, Kubernetes, or distributed locking system is required for the core reservation flow.

This keeps the implementation small while allowing PostgreSQL transactions and row-level locks to provide strong consistency.

## AI Usage

AI assistance was used during development for debugging, implementation guidance, code review, and documentation support.
