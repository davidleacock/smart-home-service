CREATE TABLE events
(
    event_id   SERIAL PRIMARY KEY,
    home_id    UUID         NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data TEXT         NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_home_id ON events (home_id);
