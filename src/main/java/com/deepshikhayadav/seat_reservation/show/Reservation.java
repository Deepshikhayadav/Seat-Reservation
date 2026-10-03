package com.deepshikhayadav.seat_reservation.show;



import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter 
@Setter 
@Entity 
@Table (name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}