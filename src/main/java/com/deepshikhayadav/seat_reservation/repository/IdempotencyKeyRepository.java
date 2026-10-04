package com.deepshikhayadav.seat_reservation.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepshikhayadav.seat_reservation.utils.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKey, UUID> {

    /*
     * This is intentionally INSERT ... ON CONFLICT.
     *
     * Two identical requests can arrive at exactly the same time.
     * A normal find-then-save is not enough because both requests
     * could observe "no row exists".
     *
     * PostgreSQL's unique constraint decides which request creates
     * the idempotency record.
     */
    @Modifying
    @Query(value = """
        INSERT INTO idempotency_keys
            (
                id,
                user_id,
                show_id,
                idempotency_key,
                request_hash,
                status,
                created_at
            )
        VALUES
            (
                :id,
                :userId,
                :showId,
                :idempotencyKey,
                :requestHash,
                'PROCESSING',
                CURRENT_TIMESTAMP
            )
        ON CONFLICT (user_id, show_id, idempotency_key)
        DO NOTHING
        """, nativeQuery = true)
    int createIfAbsent(
            @Param("id") UUID id,
            @Param("userId") String userId,
            @Param("showId") UUID showId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash
    );

    /*
     * The row is locked while this transaction processes the reservation.
     *
     * If another request is currently processing the same idempotency key,
     * PostgreSQL makes the second request wait until the first transaction
     * commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM IdempotencyKey i
        WHERE i.userId = :userId
          AND i.showId = :showId
          AND i.key = :idempotencyKey
        """)
    Optional<IdempotencyKey> findForUpdate(
            @Param("userId") String userId,
            @Param("showId") UUID showId,
            @Param("idempotencyKey") String idempotencyKey
    );
}