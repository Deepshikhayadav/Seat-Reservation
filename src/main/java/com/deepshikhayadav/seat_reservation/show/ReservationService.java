package com.deepshikhayadav.seat_reservation.show;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ReservationRepository reservationRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public ReservationService(
            SeatRepository seatRepository,
            ShowRepository showRepository,
            ReservationRepository reservationRepository,
            IdempotencyKeyRepository idempotencyKeyRepository) {

        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.reservationRepository = reservationRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public Reservation reserve(
            UUID showId,
            String userId,
            String idempotencyKey,
            ReserveRequest request) {

        // ------------------------------------------------
        // 1. Find the show
        // ------------------------------------------------

        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new RuntimeException("Show not found"));


        // ------------------------------------------------
        // 2. Normalize requested seats
        // ------------------------------------------------

        List<String> requestedSeats = request.getSeats()
                .stream()
                .distinct()
                .sorted()
                .toList();


        // ------------------------------------------------
        // 3. Create a hash of the request
        // ------------------------------------------------

        String requestHash =
                HashUtil.sha256(
                        String.join(",", requestedSeats)
                );


        // ------------------------------------------------
        // 4. Check idempotency
        // ------------------------------------------------

        Optional<IdempotencyKey> existing =
                idempotencyKeyRepository
                        .findByUserIdAndShowIdAndKey(
                                userId,
                                showId,
                                idempotencyKey
                        );

        if (existing.isPresent()) {

            IdempotencyKey record = existing.get();

            // Same key but different request
            if (!record.getRequestHash()
                    .equals(requestHash)) {

                throw new IdempotencyConflictException(
                        "Idempotency key was already used " +
                        "with a different request"
                );
            }

            // Same key + same request
            if ("CONFIRMED".equals(record.getStatus())) {

                return reservationRepository
                        .findById(record.getReservationId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Original reservation not found"
                                ));
            }

            // If we reach here, something is wrong
            // with the idempotency record.
            throw new RuntimeException(
                    "Invalid idempotency state"
            );
        }


        // ------------------------------------------------
        // 5. Create idempotency record
        // ------------------------------------------------

        IdempotencyKey idempotencyRecord =
                new IdempotencyKey();

        // idempotencyRecord.setId(UUID.randomUUID());
        idempotencyRecord.setUserId(userId);
        idempotencyRecord.setShowId(showId);
        idempotencyRecord.setKey(idempotencyKey);
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setStatus("PROCESSING");
        idempotencyRecord.setCreatedAt(LocalDateTime.now());

        idempotencyKeyRepository.save(idempotencyRecord);


        // ------------------------------------------------
        // 6. Lock requested seats
        // ------------------------------------------------

        List<Seat> seats =
                seatRepository.findSeatsForUpdate(
                        showId,
                        requestedSeats
                );


        // ------------------------------------------------
        // 7. Check all seats exist
        // ------------------------------------------------

        if (seats.size() != requestedSeats.size()) {

            throw new ReservationConflictException(
                    "SEAT_NOT_FOUND",
                    "One or more requested seats do not exist"
            );
        }


        // ------------------------------------------------
        // 8. Check all seats are available
        // ------------------------------------------------

        boolean anyTaken = seats.stream()
                .anyMatch(seat ->
                        !"AVAILABLE".equals(seat.getStatus())
                );

        if (anyTaken) {

            throw new ReservationConflictException(
                    "SEAT_TAKEN",
                    "One or more requested seats are already taken"
            );
        }


        // ------------------------------------------------
        // 9. Calculate amount
        // ------------------------------------------------

        long amount =
                show.getPricePaise()
                        * requestedSeats.size();


        // ------------------------------------------------
        // 10. Create reservation
        // ------------------------------------------------

        Reservation reservation =
                new Reservation();

        // reservation.setId(UUID.randomUUID());
        reservation.setShowId(showId);
        reservation.setUserId(userId);
        reservation.setAmountPaise(amount);
        reservation.setStatus("CONFIRMED");
        reservation.setCreatedAt(LocalDateTime.now());

        reservationRepository.save(reservation);


        // ------------------------------------------------
        // 11. Mark seats as confirmed
        // ------------------------------------------------

        for (Seat seat : seats) {
            seat.setStatus("CONFIRMED");
        }

        seatRepository.saveAll(seats);


        // ------------------------------------------------
        // 12. Store the reservation ID
        // ------------------------------------------------

        idempotencyRecord.setReservationId(
                reservation.getId()
        );

        idempotencyRecord.setStatus("CONFIRMED");

        idempotencyKeyRepository.save(
                idempotencyRecord
        );


        // ------------------------------------------------
        // 13. Return reservation
        // ------------------------------------------------

        return reservation;
    }
}