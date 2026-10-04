package com.deepshikhayadav.seat_reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepshikhayadav.seat_reservation.model.Show;

import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {
}