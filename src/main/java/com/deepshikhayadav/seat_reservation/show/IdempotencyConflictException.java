package com.deepshikhayadav.seat_reservation.show;

public class IdempotencyConflictException
        extends RuntimeException {

    public IdempotencyConflictException(
            String message) {

        super(message);
    }
}