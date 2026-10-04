-- If V2 has already created reservation_seats, replace its structure with
-- a simple UUID primary key while retaining database safety constraints.

DROP TABLE IF EXISTS reservation_seats;

CREATE TABLE reservation_seats (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    seat_id UUID NOT NULL,

    CONSTRAINT fk_reservation_seat_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT fk_reservation_seat_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats(id),

    CONSTRAINT uq_reservation_seat
        UNIQUE (reservation_id, seat_id),

    -- A seat can belong to only one reservation.
    CONSTRAINT uq_seat_reservation
        UNIQUE (seat_id)
);