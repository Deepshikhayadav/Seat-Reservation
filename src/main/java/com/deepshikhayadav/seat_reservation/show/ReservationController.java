package com.deepshikhayadav.seat_reservation.show;

import org.springframework.http.HttpStatus;
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
            ReserveRequest request) {

        /*
         * TEMPORARY USER ID.
         *
         * We will replace this with the authenticated JWT subject
         * in the authentication step.
         */
        String userId = "user-1";

        return reservationService.reserve(
                showId,
                userId,
                idempotencyKey,
                request
        );
    }
}