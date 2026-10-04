package com.deepshikhayadav.seat_reservation.model;

public class SeatResponse {

    private String seat;
    private String status;

    public SeatResponse(String seat, String status) {
        this.seat = seat;
        this.status = status;
    }

    public String getSeat() {
        return seat;
    }

    public String getStatus() {
        return status;
    }
}