package com.deepshikhayadav.seat_reservation.show;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByUserIdAndShowIdAndKey(
            String userId,
            UUID showId,
            String key
    );
}