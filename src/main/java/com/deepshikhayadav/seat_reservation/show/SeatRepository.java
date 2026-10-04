package com.deepshikhayadav.seat_reservation.show;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatRepository
        extends JpaRepository<Seat, UUID> {

    List<Seat> findByShowId(UUID showId);

    /*
     * FOR UPDATE locks the selected seat rows.
     *
     * Sorting the input seat numbers ensures concurrent multi-seat
     * requests acquire locks in a deterministic order.
     */
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
}