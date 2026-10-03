package com.deepshikhayadav.seat_reservation.show;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateShowRequest {

    @NotBlank
    private String name;

    @NotEmpty
    private List<String> seats;

    @JsonProperty("price_paise")
    @NotNull 
    @Min(1)
    private Long pricePaise;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    public Long getPricePaise() {
        return pricePaise;
    }

    public void setPricePaise(Long pricePaise) {
        this.pricePaise = pricePaise;
    }
}