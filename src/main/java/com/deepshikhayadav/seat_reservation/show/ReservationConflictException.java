package com.deepshikhayadav.seat_reservation.show;

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