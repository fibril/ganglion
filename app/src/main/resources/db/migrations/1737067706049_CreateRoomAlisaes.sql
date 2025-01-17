CREATE TABLE IF NOT EXISTS room_aliases (
    id VARCHAR(510) PRIMARY KEY NOT NULL CHECK (id ~ '^#[a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    room_id VARCHAR NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    is_canonical BOOLEAN NOT NULL DEFAULT FALSE,
    servers TEXT[] NOT NULL DEFAULT '{}',
);


CREATE INDEX room_aliases_room_id_idx ON rooms (room_id);

CREATE TRIGGER update_room_aliases_updated_at
    BEFORE UPDATE
    ON
        room_aliases
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();