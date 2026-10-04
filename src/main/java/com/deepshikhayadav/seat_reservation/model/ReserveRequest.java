package com.deepshikhayadav.seat_reservation.model;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter 
@Setter 
public class ReserveRequest {

    @NotEmpty
    private List<String> seats;


}