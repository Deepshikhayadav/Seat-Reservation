package com.deepshikhayadav.seat_reservation.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReservationMetrics {

    private final Counter confirmedReservations;

    private final Counter seatTakenDeclines;

    private final Counter perUserLimitDeclines;

    private final Counter idempotentReplays;

    private final Counter seatNotFoundDeclines;

    private final AtomicInteger availableSeats =
            new AtomicInteger(0);

    public ReservationMetrics(
            MeterRegistry meterRegistry) {

        // ---------------------------------------------------------
        // Number of successfully created reservations.
        // ---------------------------------------------------------
        confirmedReservations =
                Counter.builder("seat_reservation_confirmed_total")
                        .description("Number of confirmed reservations")
                        .register(meterRegistry);

        // ---------------------------------------------------------
        // Requests rejected because requested seat was already taken.
        // ---------------------------------------------------------
        seatTakenDeclines =
                Counter.builder("seat_reservation_declined_total")
                        .description("Reservation requests declined")
                        .tag("reason", "seat_taken")
                        .register(meterRegistry);

        // ---------------------------------------------------------
        // Requests rejected because user exceeded reservation limit.
        // ---------------------------------------------------------
        perUserLimitDeclines =
                Counter.builder("seat_reservation_declined_total")
                        .description("Reservation requests declined")
                        .tag("reason", "per_user_limit")
                        .register(meterRegistry);

        // ---------------------------------------------------------
        // Same idempotency key + same request.
        //
        // This is NOT a new reservation.
        // It is counted so we can observe retry/replay traffic.
        // ---------------------------------------------------------
        idempotentReplays =
                Counter.builder("seat_reservation_declined_total")
                        .description("Reservation requests declined or replayed")
                        .tag("reason", "idempotent_replay")
                        .register(meterRegistry);

        // ---------------------------------------------------------
        // Requested seat does not exist.
        // ---------------------------------------------------------
        seatNotFoundDeclines =
                Counter.builder("seat_reservation_declined_total")
                        .description("Reservation requests declined")
                        .tag("reason", "seat_not_found")
                        .register(meterRegistry);

        // ---------------------------------------------------------
        // Current number of available seats.
        //
        // The ReservationService updates this after reservation
        // and cancellation.
        // ---------------------------------------------------------
        Gauge.builder(
                        "seat_reservation_available_seats",
                        availableSeats,
                        AtomicInteger::get
                )
                .description("Current number of available seats")
                .register(meterRegistry);
    }

    public void confirmed() {
        confirmedReservations.increment();
    }

    public void seatTaken() {
        seatTakenDeclines.increment();
    }

    public void perUserLimit() {
        perUserLimitDeclines.increment();
    }

    public void idempotentReplay() {
        idempotentReplays.increment();
    }

    public void seatNotFound() {
        seatNotFoundDeclines.increment();
    }

    public void setAvailableSeats(int count) {
        availableSeats.set(count);
    }
}