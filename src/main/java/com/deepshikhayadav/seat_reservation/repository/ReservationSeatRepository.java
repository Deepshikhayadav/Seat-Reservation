package com.deepshikhayadav.seat_reservation.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepshikhayadav.seat_reservation.model.ReservationSeat;

import java.util.List;
import java.util.UUID;

public interface ReservationSeatRepository
        extends JpaRepository<ReservationSeat, UUID> {

    List<ReservationSeat> findByReservationId(UUID reservationId);

    // ---------------------------------------------------------
    // Lock all reservation-seat rows while cancelling.
    // ---------------------------------------------------------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT rs
        FROM ReservationSeat rs
        WHERE rs.reservationId = :reservationId
        ORDER BY rs.seatId
        """)
    List<ReservationSeat> findByReservationIdForUpdate(
            @Param("reservationId") UUID reservationId
    );
}