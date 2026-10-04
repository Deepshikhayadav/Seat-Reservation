package com.deepshikhayadav.seat_reservation.show;


import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
    name = "user_show_limits",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_user_show_limit",
            columnNames = {"show_id", "user_id"}
        )
    }
)
public class UserShowLimit {

    @Id
    private UUID id;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "reserved_count", nullable = false)
    private Integer reservedCount = 0;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getShowId() {
        return showId;
    }

    public void setShowId(UUID showId) {
        this.showId = showId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(Integer reservedCount) {
        this.reservedCount = reservedCount;
    }
}