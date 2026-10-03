package com.deepshikhayadav.seat_reservation.show;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByShowId(UUID showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Seat s
        WHERE s.showId = :showId
        AND s.seatNumber IN :seatNumbers
        ORDER BY s.seatNumber  
    """)  // normalize requested seat numbers to avoid deadlocks
    List<Seat> findSeatsForUpdate(
            @Param("showId") UUID showId,
            @Param("seatNumbers") List<String> seatNumbers
    );

}