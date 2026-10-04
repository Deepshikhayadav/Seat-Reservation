package com.deepshikhayadav.seat_reservation.model;

import java.util.List;
import java.util.UUID;

public class ShowResponse {

    private UUID id;
    private String name;
    private Long pricePaise;

    private int totalSeats;
    private int available;
    private int held;
    private int confirmed;

    private List<SeatResponse> seats;

    public ShowResponse(
            UUID id,
            String name,
            Long pricePaise,
            int totalSeats,
            int available,
            int held,
            int confirmed,
            List<SeatResponse> seats) {

        this.id = id;
        this.name = name;
        this.pricePaise = pricePaise;
        this.totalSeats = totalSeats;
        this.available = available;
        this.held = held;
        this.confirmed = confirmed;
        this.seats = seats;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getPricePaise() {
        return pricePaise;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public int getAvailable() {
        return available;
    }

    public int getHeld() {
        return held;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public List<SeatResponse> getSeats() {
        return seats;
    }
}