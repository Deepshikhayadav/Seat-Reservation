package com.deepshikhayadav.seat_reservation.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepshikhayadav.seat_reservation.model.Seat;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByShowId(UUID showId);

    // ---------------------------------------------------------
    // Lock requested seats during reservation.
    // ---------------------------------------------------------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Seat s
        WHERE s.showId = :showId
          AND s.seatNumber IN :seatNumbers
        ORDER BY s.seatNumber
        """)
    List<Seat> findSeatsForUpdate(
            @Param("showId") UUID showId,
            @Param("seatNumbers") List<String> seatNumbers
    );

    // ---------------------------------------------------------
    // Lock seats during cancellation.
    // ---------------------------------------------------------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Seat s
        WHERE s.id IN :seatIds
        ORDER BY s.id
        """)
    List<Seat> findSeatsByIdsForUpdate(
            @Param("seatIds") List<UUID> seatIds
    );

    // ---------------------------------------------------------
    // Count currently available seats.
    // ---------------------------------------------------------
    long countByStatus(String status);

    // ---------------------------------------------------------
    // Count available seats for one show.
    // ---------------------------------------------------------
    long countByShowIdAndStatus(
            UUID showId,
            String status
    );
}