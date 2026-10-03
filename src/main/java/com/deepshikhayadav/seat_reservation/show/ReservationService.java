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

    public ReservationService(
            SeatRepository seatRepository,
            ShowRepository showRepository,
            ReservationRepository reservationRepository) {

        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation reserve(
            UUID showId,
            String userId,
            ReserveRequest request) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new RuntimeException("Show not found"));

        List<String> requestedSeats = request.getSeats()
                .stream()
                .distinct()
                .sorted()
                .toList();

        List<Seat> seats =
                seatRepository.findSeatsForUpdate(
                        showId,
                        requestedSeats
                );

        if (seats.size() != requestedSeats.size()) {
            throw new RuntimeException(
                    "One or more seats do not exist"
            );
        }

        boolean anyTaken = seats.stream()
                .anyMatch(seat ->
                        !seat.getStatus().equals("AVAILABLE")
                );

        if (anyTaken) {
            throw new RuntimeException(
                    "One or more seats are already taken"
            );
        }

        long amount =
                show.getPricePaise() * requestedSeats.size();

        Reservation reservation = new Reservation();

        // reservation.setId(UUID.randomUUID());
        reservation.setShowId(showId);
        reservation.setUserId(userId);
        reservation.setAmountPaise(amount);
        reservation.setStatus("CONFIRMED");
        reservation.setCreatedAt(LocalDateTime.now());

        reservationRepository.save(reservation);

        for (Seat seat : seats) {
            seat.setStatus("CONFIRMED");
        }

        seatRepository.saveAll(seats);

        return reservation;
    }
}