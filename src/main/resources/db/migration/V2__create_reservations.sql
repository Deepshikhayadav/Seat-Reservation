CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    amount_paise BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_reservation_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id)
);

CREATE TABLE reservation_seats (
    reservation_id UUID NOT NULL,
    seat_id UUID NOT NULL,

    PRIMARY KEY (reservation_id, seat_id),

    CONSTRAINT fk_reservation_seat_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT fk_reservation_seat_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats(id),

    CONSTRAINT uq_reservation_seat
        UNIQUE (seat_id)   -- if there's a bug in our application code, PostgreSQL provides another safety net.
);