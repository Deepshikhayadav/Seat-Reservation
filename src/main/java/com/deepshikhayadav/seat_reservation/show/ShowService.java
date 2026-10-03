package com.deepshikhayadav.seat_reservation.show;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowService(
            ShowRepository showRepository,
            SeatRepository seatRepository) {

        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Show createShow(CreateShowRequest request) {

        Show show = new Show();

        // show.setId(UUID.randomUUID());
        show.setName(request.getName());
        show.setPricePaise(request.getPricePaise());
        show.setPerUserLimit(4);
        show.setCreatedAt(LocalDateTime.now());

        showRepository.save(show);

        List<Seat> seats = new ArrayList<>();

        for (String seatNumber : request.getSeats()) {

            Seat seat = new Seat();

            // seat.setId(UUID.randomUUID());
            seat.setShowId(show.getId());
            seat.setSeatNumber(seatNumber);
            seat.setStatus("AVAILABLE");
            // seat.setVersion(0L);

            seats.add(seat);
        }

        seatRepository.saveAll(seats);

        return show;
    }
}