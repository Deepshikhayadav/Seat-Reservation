CREATE TABLE shows (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_paise BIGINT NOT NULL,
    per_user_limit INT NOT NULL DEFAULT 4,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE seats (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_seat_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT uq_show_seat
        UNIQUE (show_id, seat_number)
);

CREATE INDEX idx_seats_show_id
    ON seats(show_id);