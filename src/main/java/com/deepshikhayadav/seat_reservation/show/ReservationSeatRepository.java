package com.deepshikhayadav.seat_reservation.show;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationSeatRepository
        extends JpaRepository<ReservationSeat, UUID> {

    List<ReservationSeat> findByReservationId(UUID reservationId);
}