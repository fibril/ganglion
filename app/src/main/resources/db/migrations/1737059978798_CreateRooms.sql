CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(510) PRIMARY KEY NOT NULL CHECK (id ~ '^![a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    creator_id VARCHAR NOT NULL REFERENCES users (id),
    avatar_id VARCHAR REFERENCES media (id),
    is_direct BOOLEAN NOT NULL DEFAULT FALSE,
    type VARCHAR,
    version VARCHAR(5) NOT NULL DEFAULT '11',
    visibility VARCHAR(10) NOT NULL DEFAULT 'private',
    CONSTRAINT check_room_visibility_type CHECK(visibility IN('private', 'public'))
);


CREATE INDEX rooms_user_id_idx ON rooms (creator_id);

CREATE TRIGGER update_rooms_updated_at
    BEFORE UPDATE
    ON
        rooms
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();