package com.deepshikhayadav.seat_reservation.show;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_show_seat",
                        columnNames = {"show_id", "seat_number"}
                )
        }
)
public class Seat {

    @Id
    private UUID id;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private String status;

    @Version
    private Long version;

    // getters and setters
}