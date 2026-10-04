package com.deepshikhayadav.seat_reservation.show;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository
        extends JpaRepository<Reservation, UUID> {

    // ---------------------------------------------------------
    // Lock the reservation row while cancelling it.
    // This prevents two concurrent cancellation requests
    // from modifying the same reservation at the same time.
    // ---------------------------------------------------------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.id = :reservationId
        """)
    Optional<Reservation> findForUpdate(
            @Param("reservationId") UUID reservationId
    );
}