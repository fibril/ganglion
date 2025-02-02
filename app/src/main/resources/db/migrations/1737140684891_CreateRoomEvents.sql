CREATE TABLE IF NOT EXISTS room_events (
    id VARCHAR(510) PRIMARY KEY NOT NULL CHECK (id ~ '^\$[a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    sender VARCHAR NOT NULL REFERENCES users (id),
    room_id VARCHAR NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    content JSONB NOT NULL,
    state_key VARCHAR(510),
    type VARCHAR(255) NOT NULL,
    CONSTRAINT room_events_unique_room_type_state_key UNIQUE (room_id, state_key, type)
);


CREATE INDEX room_events_sender_idx on room_events(sender);
CREATE INDEX room_events_room_id_idx on room_events(room_id);
CREATE INDEX room_events_room_id_type_idx on room_events(room_id, type);

CREATE TRIGGER update_room_events_updated_at
    BEFORE UPDATE
    ON
        room_events
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();