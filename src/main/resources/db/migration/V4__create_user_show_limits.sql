CREATE TABLE user_show_limits (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    reserved_count INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_user_show_limit_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT uq_user_show_limit
        UNIQUE (show_id, user_id)
);