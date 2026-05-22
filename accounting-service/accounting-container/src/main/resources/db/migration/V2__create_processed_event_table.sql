CREATE TABLE IF NOT EXISTS platform_processed_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);
