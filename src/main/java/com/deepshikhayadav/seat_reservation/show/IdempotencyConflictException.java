package com.deepshikhayadav.seat_reservation.show;


public class IdempotencyConflictException
        extends ReservationConflictException {

    public IdempotencyConflictException(
            String message) {

        super(
                "IDEMPOTENCY_CONFLICT",
                message
        );
    }
}