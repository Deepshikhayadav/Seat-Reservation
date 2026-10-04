package com.deepshikhayadav.seat_reservation.show;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter 
@Setter 
@Entity 
@Table (
    name = "idempotency_keys",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_idempotency",
            columnNames = {
                "user_id",
                "show_id",
                "idempotency_key"
            }
        )
    }
)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "idempotency_key", nullable = false)
    private String key;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}