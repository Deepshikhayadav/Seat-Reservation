package com.deepshikhayadav.seat_reservation.utils;

public class ReservationConflictException
        extends RuntimeException {

    private final String code;

    public ReservationConflictException(
            String code,
            String message) {

        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}