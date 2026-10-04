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

    public CancellationService(
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            SeatRepository seatRepository,
            UserShowLimitRepository userShowLimitRepository) {

        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.seatRepository = seatRepository;
        this.userShowLimitRepository = userShowLimitRepository;
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
        // 2. Only the reservation owner can cancel it.
        // ---------------------------------------------------------
        if (!reservation.getUserId().equals(userId)) {
            throw new ReservationConflictException(
                    "FORBIDDEN",
                    "You cannot cancel another user's reservation"
            );
        }

        // ---------------------------------------------------------
        // 3. Cancellation is idempotent.
        //
        // If the same user sends cancellation again, simply return
        // the already-cancelled reservation.
        // ---------------------------------------------------------
        if ("CANCELLED".equals(reservation.getStatus())) {
            return reservation;
        }

        // ---------------------------------------------------------
        // Only confirmed reservations can be cancelled.
        // ---------------------------------------------------------
        if (!"CONFIRMED".equals(reservation.getStatus())) {
            throw new ReservationConflictException(
                    "INVALID_RESERVATION_STATE",
                    "Only confirmed reservations can be cancelled"
            );
        }

        // ---------------------------------------------------------
        // 4. Lock reservation-seat rows.
        // ---------------------------------------------------------
        List<ReservationSeat> reservationSeats =
                reservationSeatRepository
                        .findByReservationIdForUpdate(reservationId);

        if (reservationSeats.isEmpty()) {
            throw new IllegalStateException(
                    "Reservation has no seats"
            );
        }

        // ---------------------------------------------------------
        // 5. Collect seat IDs.
        // ---------------------------------------------------------
        List<UUID> seatIds =
                reservationSeats.stream()
                        .map(ReservationSeat::getSeatId)
                        .sorted()
                        .toList();

        // ---------------------------------------------------------
        // 6. Lock the actual seat rows.
        // ---------------------------------------------------------
        List<Seat> seats =
                seatRepository.findSeatsByIdsForUpdate(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new IllegalStateException(
                    "One or more reservation seats no longer exist"
            );
        }

        // ---------------------------------------------------------
        // 7. Make seats available again.
        // ---------------------------------------------------------
        for (Seat seat : seats) {
            seat.setStatus("AVAILABLE");
        }

        seatRepository.saveAll(seats);

        // ---------------------------------------------------------
        // 8. Decrease user's reserved-seat counter.
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
                                )
                        );

        int newCount =
                userLimit.getReservedCount() - seats.size();

        // Defensive protection against invalid negative values.
        userLimit.setReservedCount(
                Math.max(newCount, 0)
        );

        userShowLimitRepository.save(userLimit);

        // ---------------------------------------------------------
        // 9. Mark reservation cancelled.
        // ---------------------------------------------------------
        reservation.setStatus("CANCELLED");

        reservationRepository.save(reservation);

        // ---------------------------------------------------------
        // Because everything is inside one transaction:
        //
        // seats + user count + reservation status
        //
        // either all commit or all roll back.
        // ---------------------------------------------------------
        return reservation;
    }
}