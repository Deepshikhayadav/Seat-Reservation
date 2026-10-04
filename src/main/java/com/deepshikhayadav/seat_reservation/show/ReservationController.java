package com.deepshikhayadav.seat_reservation.show;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shows")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/{showId}/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation reserve(
            @PathVariable UUID showId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @RequestBody
            ReserveRequest request,

            Authentication authentication) {

       
        String userId = authentication.getName();

        return reservationService.reserve(
                showId,
                userId,
                idempotencyKey,
                request
        );
    }
}