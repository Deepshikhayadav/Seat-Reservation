package com.deepshikhayadav.seat_reservation.show;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CancellationService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final SeatRepository seatRepository;
    private final UserShowLimitRepository userShowLimitRepository;
    private final ReservationMetrics reservationMetrics;

    public CancellationService(
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            SeatRepository seatRepository,
            UserShowLimitRepository userShowLimitRepository,
            ReservationMetrics reservationMetrics) {

        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.seatRepository = seatRepository;
        this.userShowLimitRepository = userShowLimitRepository;
        this.reservationMetrics = reservationMetrics;
    }

    @Transactional
    public Reservation cancel(
            UUID reservationId,
            String userId) {

        // ---------------------------------------------------------
        // 1. Lock reservation.
        // ---------------------------------------------------------
        Reservation reservation =
                reservationRepository
                        .findForUpdate(reservationId)
                        .orElseThrow(() ->
                                new ReservationConflictException(
                                        "RESERVATION_NOT_FOUND",
                                        "Reservation not found"
                                )
                        );

        // ---------------------------------------------------------
        // 2. Only owner can cancel.
        // ---------------------------------------------------------
        if (!reservation
                .getUserId()
                .equals(userId)) {

            throw new ReservationConflictException(
                    "FORBIDDEN",
                    "You cannot cancel another user's reservation"
            );
        }

        // ---------------------------------------------------------
        // 3. Cancellation is idempotent.
        // ---------------------------------------------------------
        if ("CANCELLED".equals(
                reservation.getStatus())) {

            return reservation;
        }

        // ---------------------------------------------------------
        // 4. Only confirmed reservations can be cancelled.
        // ---------------------------------------------------------
        if (!"CONFIRMED".equals(
                reservation.getStatus())) {

            throw new ReservationConflictException(
                    "INVALID_RESERVATION_STATE",
                    "Only confirmed reservations can be cancelled"
            );
        }

        // ---------------------------------------------------------
        // 5. Lock reservation-seat rows.
        // ---------------------------------------------------------
        List<ReservationSeat> reservationSeats =
                reservationSeatRepository
                        .findByReservationIdForUpdate(
                                reservationId
                        );

        if (reservationSeats.isEmpty()) {

            throw new IllegalStateException(
                    "Reservation has no seats"
            );
        }

        // ---------------------------------------------------------
        // 6. Get seat IDs in deterministic order.
        // ---------------------------------------------------------
        List<UUID> seatIds =
                reservationSeats.stream()
                        .map(ReservationSeat::getSeatId)
                        .sorted()
                        .toList();

        // ---------------------------------------------------------
        // 7. Lock actual seat rows.
        // ---------------------------------------------------------
        List<Seat> seats =
                seatRepository.findSeatsByIdsForUpdate(
                        seatIds
                );

        if (seats.size() != seatIds.size()) {

            throw new IllegalStateException(
                    "One or more reservation seats no longer exist"
            );
        }

        // ---------------------------------------------------------
        // 8. Release seats.
        // ---------------------------------------------------------
        for (Seat seat : seats) {
            seat.setStatus("AVAILABLE");
        }

        seatRepository.saveAll(seats);

        // ---------------------------------------------------------
        // 9. Decrease user's reserved-seat count.
        // ---------------------------------------------------------
        UserShowLimit userLimit =
                userShowLimitRepository
                        .findForUpdate(
                                reservation.getShowId(),
                                reservation.getUserId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "User limit row not found"
                                ));

        int newCount =
                userLimit.getReservedCount()
                        - seats.size();

        userLimit.setReservedCount(
                Math.max(newCount, 0)
        );

        userShowLimitRepository.save(
                userLimit
        );

        // ---------------------------------------------------------
        // 10. Mark reservation cancelled.
        // ---------------------------------------------------------
        reservation.setStatus(
                "CANCELLED"
        );

        reservationRepository.save(
                reservation
        );

        // ---------------------------------------------------------
        // 11. Update available-seat gauge.
        // ---------------------------------------------------------
        reservationMetrics.setAvailableSeats(
                (int) seatRepository.countByStatus(
                        "AVAILABLE"
                )
        );

        return reservation;
    }
}