CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(510) PRIMARY KEY NOT NULL CHECK (id ~ '^![a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    user_id VARCHAR NOT NULL REFERENCES users (id),
    avatar_id VARCHAR REFERENCES media (id),
    allow_room_ids TEXT[] NOT NULL DEFAULT '{}',
    guest_can_join BOOLEAN NOT NULL DEFAULT FALSE,
    is_direct BOOLEAN NOT NULL DEFAULT FALSE,
    join_rule VARCHAR(20) NOT NULL,
    name VARCHAR(255),
    room_type VARCHAR,
    topic VARCHAR(255),
    visibility VARCHAR(10) NOT NULL DEFAULT 'public',
    world_readable BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT check_room_visibility_type CHECK(visibility IN('private', 'public'))
    CONSTRAINT check_join_rule CHECK(join_rule IN('invite', 'knock', 'knock_restricted', 'public', 'restricted'))
);


CREATE INDEX rooms_user_id_idx ON rooms (user_id);

CREATE TRIGGER update_rooms_updated_at
    BEFORE UPDATE
    ON
        rooms
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();