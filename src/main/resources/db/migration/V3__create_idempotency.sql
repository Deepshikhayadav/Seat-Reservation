CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,

    user_id VARCHAR(100) NOT NULL,

    show_id UUID NOT NULL,

    idempotency_key VARCHAR(255) NOT NULL,

    request_hash VARCHAR(64) NOT NULL,

    reservation_id UUID,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_idempotency
        UNIQUE (user_id, show_id, idempotency_key),

    CONSTRAINT fk_idempotency_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT fk_idempotency_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id)
);