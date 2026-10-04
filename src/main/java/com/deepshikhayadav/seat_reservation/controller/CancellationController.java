package com.deepshikhayadav.seat_reservation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.deepshikhayadav.seat_reservation.model.Reservation;
import com.deepshikhayadav.seat_reservation.services.CancellationService;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class CancellationController {

    private final CancellationService cancellationService;

    public CancellationController(
            CancellationService cancellationService) {

        this.cancellationService = cancellationService;
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<Reservation> cancel(
            @PathVariable UUID reservationId,
            Authentication authentication) {

        // ---------------------------------------------------------
        // User identity comes from the validated JWT.
        // ---------------------------------------------------------
        String userId = authentication.getName();

        Reservation reservation =
                cancellationService.cancel(
                        reservationId,
                        userId
                );

        return ResponseEntity.ok(reservation);
    }
}