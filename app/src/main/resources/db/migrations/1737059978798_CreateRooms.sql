CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(510) PRIMARY KEY NOT NULL CHECK (id ~ '^![a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    creator_id VARCHAR NOT NULL REFERENCES users (id),
    avatar_id VARCHAR REFERENCES media (id),
    canonical_alias VARCHAR,
    guest_access VARCHAR(10),
    is_direct BOOLEAN NOT NULL DEFAULT FALSE,
    join_rule VARCHAR,
    name VARCHAR,
    num_joined_members INTEGER NOT NULL DEFAULT 0,
    type VARCHAR,
    topic VARCHAR,
    version VARCHAR(5) NOT NULL DEFAULT '11',
    visibility VARCHAR(10) NOT NULL DEFAULT 'private',
    history_visibility VARCHAR DEFAULT 'joined',
    CONSTRAINT check_room_guest_access_type CHECK(guest_access IN('can_join', 'forbidden')),
    CONSTRAINT check_room_history_visibility_type CHECK(history_visibility IN('invited', 'joined', 'shared', 'world_readable')),
    CONSTRAINT check_room_visibility_type CHECK(visibility IN('private', 'public'))
);


CREATE INDEX rooms_user_id_idx ON rooms (creator_id);

CREATE TRIGGER update_rooms_updated_at
    BEFORE UPDATE
    ON
        rooms
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER notify_rooms_resource_created
    AFTER INSERT
    ON
        rooms
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_created();

CREATE TRIGGER notify_rooms_resource_updated
    AFTER UPDATE
    ON
        rooms
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_updated();

CREATE TRIGGER notify_rooms_resource_deleted
    AFTER DELETE
    ON
        rooms
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_deleted();