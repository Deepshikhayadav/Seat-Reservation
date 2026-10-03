package com.deepshikhayadav.seat_reservation.show;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;


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
            @RequestBody ReserveRequest request) {

        // TEMPORARY USER
        String userId = "user-1";

        return reservationService.reserve(
                showId,
                userId,
                request
        );
    }
}