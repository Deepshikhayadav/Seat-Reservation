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
    private final ReservationMetrics reservationMetrics;

    public ReservationService(
            SeatRepository seatRepository,
            ShowRepository showRepository,
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            UserShowLimitRepository userShowLimitRepository,
            ReservationMetrics reservationMetrics) {

        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.userShowLimitRepository = userShowLimitRepository;
        this.reservationMetrics = reservationMetrics;
    }

    @Transactional
    public Reservation reserve(
            UUID showId,
            String userId,
            String idempotencyKey,
            ReserveRequest request) {

        // ---------------------------------------------------------
        // 1. Validate show.
        // ---------------------------------------------------------
        Show show =
                showRepository.findById(showId)
                        .orElseThrow(() ->
                                new RuntimeException("Show not found"));

        // ---------------------------------------------------------
        // 2. Validate idempotency key.
        // ---------------------------------------------------------
        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        // ---------------------------------------------------------
        // 3. Normalize requested seats.
        //
        // Sorting is important because concurrent requests acquire
        // seat locks in the same order.
        // ---------------------------------------------------------
        List<String> requestedSeats =
                request.getSeats()
                        .stream()
                        .distinct()
                        .sorted()
                        .toList();

        if (requestedSeats.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one seat is required"
            );
        }

        int requestedCount =
                requestedSeats.size();

        // ---------------------------------------------------------
        // 4. Hash normalized request.
        // ---------------------------------------------------------
        String requestHash =
                HashUtil.sha256(
                        String.join(",", requestedSeats)
                );

        // ---------------------------------------------------------
        // 5. Create idempotency record if it does not exist.
        // ---------------------------------------------------------
        idempotencyKeyRepository.createIfAbsent(
                UUID.randomUUID(),
                userId,
                showId,
                idempotencyKey,
                requestHash
        );

        // ---------------------------------------------------------
        // 6. Lock idempotency row.
        // ---------------------------------------------------------
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

        // ---------------------------------------------------------
        // 7. Same key but different request.
        // ---------------------------------------------------------
        if (!idempotencyRecord
                .getRequestHash()
                .equals(requestHash)) {

            throw new ReservationConflictException(
                    "IDEMPOTENCY_CONFLICT",
                    "Idempotency key was already used with a different request"
            );
        }

        // ---------------------------------------------------------
        // 8. Same key + same request.
        //
        // Return the original reservation.
        // Do NOT create another reservation.
        // ---------------------------------------------------------
        if ("CONFIRMED".equals(
                idempotencyRecord.getStatus())) {

            reservationMetrics.idempotentReplay();

            return reservationRepository
                    .findById(
                            idempotencyRecord.getReservationId()
                    )
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Original reservation not found"
                            ));
        }

        // ---------------------------------------------------------
        // 9. Create/lock per-user limit row.
        // ---------------------------------------------------------
        userShowLimitRepository.createIfAbsent(
                UUID.randomUUID(),
                showId,
                userId
        );

        UserShowLimit userLimit =
                userShowLimitRepository
                        .findForUpdate(
                                showId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "User limit row not found"
                                ));

        // ---------------------------------------------------------
        // 10. Enforce per-user limit.
        // ---------------------------------------------------------
        int currentCount =
                userLimit.getReservedCount();

        int maxAllowed =
                show.getPerUserLimit();

        if (currentCount + requestedCount >
                maxAllowed) {

            reservationMetrics.perUserLimit();

            throw new ReservationConflictException(
                    "PER_USER_LIMIT",
                    "User reservation limit exceeded"
            );
        }

        // ---------------------------------------------------------
        // 11. Lock requested seats.
        // ---------------------------------------------------------
        List<Seat> seats =
                seatRepository.findSeatsForUpdate(
                        showId,
                        requestedSeats
                );

        // ---------------------------------------------------------
        // 12. Check that every requested seat exists.
        // ---------------------------------------------------------
        if (seats.size() != requestedCount) {

            reservationMetrics.seatNotFound();

            throw new ReservationConflictException(
                    "SEAT_NOT_FOUND",
                    "One or more requested seats do not exist"
            );
        }

        // ---------------------------------------------------------
        // 13. Check availability.
        // ---------------------------------------------------------
        boolean anyTaken =
                seats.stream()
                        .anyMatch(seat ->
                                !"AVAILABLE".equals(
                                        seat.getStatus()
                                ));

        if (anyTaken) {

            reservationMetrics.seatTaken();

            throw new ReservationConflictException(
                    "SEAT_TAKEN",
                    "One or more requested seats are already taken"
            );
        }

        // ---------------------------------------------------------
        // 14. Calculate amount using integer paise.
        // ---------------------------------------------------------
        long amount =
                show.getPricePaise() * requestedCount;

        // ---------------------------------------------------------
        // 15. Create reservation.
        // ---------------------------------------------------------
        Reservation reservation =
                new Reservation();

        // reservation.setId(UUID.randomUUID());

        reservation.setShowId(showId);

        reservation.setUserId(userId);

        reservation.setAmountPaise(amount);

        reservation.setStatus("CONFIRMED");

        reservation.setCreatedAt(
                LocalDateTime.now()
        );

        reservationRepository.save(
                reservation
        );

        // ---------------------------------------------------------
        // 16. Create reservation-seat rows.
        // ---------------------------------------------------------
        List<ReservationSeat> reservationSeats =
                seats.stream()
                        .map(seat -> {

                            ReservationSeat rs =
                                    new ReservationSeat();

                            rs.setId(
                                    UUID.randomUUID()
                            );

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

        // ---------------------------------------------------------
        // 17. Mark seats confirmed.
        // ---------------------------------------------------------
        for (Seat seat : seats) {
            seat.setStatus("CONFIRMED");
        }

        seatRepository.saveAll(seats);

        // ---------------------------------------------------------
        // 18. Increment user reservation count.
        // ---------------------------------------------------------
        userLimit.setReservedCount(
                currentCount + requestedCount
        );

        userShowLimitRepository.save(
                userLimit
        );

        // ---------------------------------------------------------
        // 19. Complete idempotency record.
        // ---------------------------------------------------------
        idempotencyRecord.setReservationId(
                reservation.getId()
        );

        idempotencyRecord.setStatus(
                "CONFIRMED"
        );

        idempotencyKeyRepository.save(
                idempotencyRecord
        );

        // ---------------------------------------------------------
        // 20. Metrics.
        // ---------------------------------------------------------
        reservationMetrics.confirmed();

        // Update global available-seat gauge.
        reservationMetrics.setAvailableSeats(
                (int) seatRepository.countByStatus(
                        "AVAILABLE"
                )
        );

        return reservation;
    }
}