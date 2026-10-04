package com.deepshikhayadav.seat_reservation.show;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserShowLimitRepository userShowLimitRepository;

    public ReservationService(
            SeatRepository seatRepository,
            ShowRepository showRepository,
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            UserShowLimitRepository userShowLimitRepository) {

        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.userShowLimitRepository = userShowLimitRepository;
    }

    @Transactional
    public Reservation reserve(
            UUID showId,
            String userId,
            String idempotencyKey,
            ReserveRequest request) {

        // ------------------------------------------------------------
        // 1. Validate show
        // ------------------------------------------------------------

        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new RuntimeException("Show not found"));

        // ------------------------------------------------------------
        // 2. Validate idempotency key
        // ------------------------------------------------------------

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        // ------------------------------------------------------------
        // 3. Normalize seats
        //
        // Sorting is important because concurrent multi-seat requests
        // must acquire seat locks in the same order.
        // ------------------------------------------------------------

        List<String> requestedSeats = request.getSeats()
                .stream()
                .distinct()
                .sorted()
                .toList();

        if (requestedSeats.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one seat is required"
            );
        }

        int requestedCount = requestedSeats.size();

        // ------------------------------------------------------------
        // 4. Create deterministic hash of the request body
        //
        // ["A2", "A1"] and ["A1", "A2"] become the same request.
        // ------------------------------------------------------------

        String requestHash = HashUtil.sha256(
                String.join(",", requestedSeats)
        );

        // ------------------------------------------------------------
        // 5. Create idempotency record if this is a new key.
        //
        // PostgreSQL unique constraint makes this safe when many
        // identical requests arrive concurrently.
        // ------------------------------------------------------------

        idempotencyKeyRepository.createIfAbsent(
                UUID.randomUUID(),
                userId,
                showId,
                idempotencyKey,
                requestHash
        );

        // ------------------------------------------------------------
        // 6. Lock the idempotency row.
        // ------------------------------------------------------------

        IdempotencyKey idempotencyRecord =
                idempotencyKeyRepository
                        .findForUpdate(
                                userId,
                                showId,
                                idempotencyKey
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Idempotency record not found"
                                ));

        // ------------------------------------------------------------
        // 7. Same key + different request = 409
        // ------------------------------------------------------------

        if (!idempotencyRecord.getRequestHash()
                .equals(requestHash)) {

            throw new ReservationConflictException(
                    "IDEMPOTENCY_CONFLICT",
                    "Idempotency key was already used " +
                    "with a different request"
            );
        }

        // ------------------------------------------------------------
        // 8. Same key + same request + already confirmed
        //
        // This is a normal retry.
        // Return the original reservation.
        // ------------------------------------------------------------

        if ("CONFIRMED".equals(
                idempotencyRecord.getStatus())) {

            return reservationRepository
                    .findById(
                            idempotencyRecord.getReservationId()
                    )
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Original reservation not found"
                            ));
        }

        // ------------------------------------------------------------
        // 9. Lock/create user's per-show counter row
        // ------------------------------------------------------------

        userShowLimitRepository.createIfAbsent(
                UUID.randomUUID(),
                showId,
                userId
        );

        UserShowLimit userLimit =
                userShowLimitRepository
                        .findForUpdate(showId, userId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "User limit row not found"
                                ));

        // ------------------------------------------------------------
        // 10. Check per-user limit
        //
        // All requested seats are accepted or all are rejected.
        // ------------------------------------------------------------

        int currentCount =
                userLimit.getReservedCount();

        int maxAllowed =
                show.getPerUserLimit();

        if (currentCount + requestedCount > maxAllowed) {

            throw new ReservationConflictException(
                    "PER_USER_LIMIT",
                    "User reservation limit exceeded"
            );
        }

        // ------------------------------------------------------------
        // 11. Lock all requested seats.
        //
        // SeatRepository uses PESSIMISTIC_WRITE / FOR UPDATE.
        // ------------------------------------------------------------

        List<Seat> seats =
                seatRepository.findSeatsForUpdate(
                        showId,
                        requestedSeats
                );

        // ------------------------------------------------------------
        // 12. Every requested seat must exist.
        // ------------------------------------------------------------

        if (seats.size() != requestedCount) {

            throw new ReservationConflictException(
                    "SEAT_NOT_FOUND",
                    "One or more requested seats do not exist"
            );
        }

        // ------------------------------------------------------------
        // 13. All-or-nothing semantics.
        //
        // If even one seat is already taken, the entire request
        // is rejected.
        // ------------------------------------------------------------

        boolean anyTaken = seats.stream()
                .anyMatch(seat ->
                        !"AVAILABLE".equals(
                                seat.getStatus()
                        ));

        if (anyTaken) {

            throw new ReservationConflictException(
                    "SEAT_TAKEN",
                    "One or more requested seats are already taken"
            );
        }

        // ------------------------------------------------------------
        // 14. Calculate price using integer paise.
        // ------------------------------------------------------------

        long amount =
                show.getPricePaise() * requestedCount;

        // ------------------------------------------------------------
        // 15. Create reservation.
        // ------------------------------------------------------------

        Reservation reservation =
                new Reservation();

        // reservation.setId(UUID.randomUUID());
        reservation.setShowId(showId);
        reservation.setUserId(userId);
        reservation.setAmountPaise(amount);
        reservation.setStatus("CONFIRMED");
        reservation.setCreatedAt(LocalDateTime.now());

        reservationRepository.save(reservation);

        // ------------------------------------------------------------
        // 16. Store reservation -> seat relationships.
        // ------------------------------------------------------------

        List<ReservationSeat> reservationSeats =
                seats.stream()
                        .map(seat -> {

                            ReservationSeat rs =
                                    new ReservationSeat();

                            rs.setId(UUID.randomUUID());

                            rs.setReservationId(
                                    reservation.getId()
                            );

                            rs.setSeatId(
                                    seat.getId()
                            );

                            return rs;
                        })
                        .toList();

        reservationSeatRepository.saveAll(
                reservationSeats
        );

        // ------------------------------------------------------------
        // 17. Mark seats as confirmed.
        // ------------------------------------------------------------

        for (Seat seat : seats) {
            seat.setStatus("CONFIRMED");
        }

        seatRepository.saveAll(seats);

        // ------------------------------------------------------------
        // 18. Increase user's reserved seat counter.
        // ------------------------------------------------------------

        userLimit.setReservedCount(
                currentCount + requestedCount
        );

        userShowLimitRepository.save(userLimit);

        // ------------------------------------------------------------
        // 19. Complete idempotency record.
        // ------------------------------------------------------------

        idempotencyRecord.setReservationId(
                reservation.getId()
        );

        idempotencyRecord.setStatus("CONFIRMED");

        idempotencyKeyRepository.save(
                idempotencyRecord
        );

        // ------------------------------------------------------------
        // 20. Transaction commits automatically here.
        //
        // If anything above throws an exception, the whole transaction
        // rolls back:
        //
        // reservation
        // reservation_seats
        // seat status
        // user counter
        // idempotency record
        // ------------------------------------------------------------

        return reservation;
    }
}