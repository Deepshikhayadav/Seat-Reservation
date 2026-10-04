package com.deepshikhayadav.seat_reservation.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepshikhayadav.seat_reservation.utils.UserShowLimit;

import java.util.Optional;
import java.util.UUID;

public interface UserShowLimitRepository
        extends JpaRepository<UserShowLimit, UUID> {

    /*
     * Creates the counter row if this is the first reservation
     * attempt by this user for this show.
     */
    @Modifying
    @Query(value = """
        INSERT INTO user_show_limits
            (
                id,
                show_id,
                user_id,
                reserved_count
            )
        VALUES
            (
                :id,
                :showId,
                :userId,
                0
            )
        ON CONFLICT (show_id, user_id)
        DO NOTHING
        """, nativeQuery = true)
    int createIfAbsent(
            @Param("id") UUID id,
            @Param("showId") UUID showId,
            @Param("userId") String userId
    );

    /*
     * Locks this user's counter row.
     *
     * This prevents two concurrent requests from both reading
     * the same reserved_count and exceeding the limit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM UserShowLimit u
        WHERE u.showId = :showId
          AND u.userId = :userId
        """)
    Optional<UserShowLimit> findForUpdate(
            @Param("showId") UUID showId,
            @Param("userId") String userId
    );
}