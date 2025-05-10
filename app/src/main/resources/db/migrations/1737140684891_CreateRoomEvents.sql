CREATE TABLE IF NOT EXISTS room_events (
    id VARCHAR(510) PRIMARY KEY NOT NULL CHECK (id ~ '^\$[a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    sender VARCHAR NOT NULL REFERENCES users (id),
    room_id VARCHAR NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    content JSONB NOT NULL,
    state_key VARCHAR(510),
    type VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    parent_id VARCHAR(510) CHECK (parent_id ~ '^\$[a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    rel_type VARCHAR CHECK (rel_type IN('m.thread', 'm.replace', 'm.annotation', 'm.reference', 'm.in_reply_to')),
    CONSTRAINT room_events_unique_room_type_state_key UNIQUE (room_id, state_key, type),
    CONSTRAINT room_events_unique_transaction_id UNIQUE (transaction_id)
);


CREATE INDEX room_events_sender_idx on room_events(sender);
CREATE INDEX room_events_room_id_idx on room_events(room_id);
CREATE INDEX room_events_room_id_type_idx on room_events(room_id, type);
CREATE INDEX room_events_content_idx on room_events USING GIN(content);

CREATE TRIGGER update_room_events_updated_at
    BEFORE UPDATE
    ON
        room_events
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER notify_room_events_resource_created
    AFTER INSERT
    ON
        room_events
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_created();

CREATE TRIGGER notify_room_events_resource_updated
    AFTER UPDATE
    ON
        room_events
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_updated();

CREATE TRIGGER notify_room_events_resource_deleted
    AFTER DELETE
    ON
        room_events
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_deleted();