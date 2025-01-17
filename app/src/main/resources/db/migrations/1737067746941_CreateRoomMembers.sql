CREATE TABLE IF NOT EXISTS room_members (
    id VARCHAR PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    room_id VARCHAR NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    user_id VARCHAR NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    power_level SMALLINT NOT NULL DEFAULT 0,
    state VARCHAR(10) NOT NULL DEFAULT 'forgotten',
    CONSTRAINT unique_user_per_room UNIQUE (user_id, room_id),
    CONSTRAINT check_power_level CHECK(power_level IN(0, 50, 100)),
    CONSTRAINT check_room_member_state CHECK(state IN (
        'banned',
        'forgotten',
        'invited',
        'joined'
        'knocking',
        'left'
    ))
);


CREATE INDEX room_members_room_id_idx ON room_members (room_id);
CREATE INDEX room_members_user_id_idx ON room_members (user_id);

CREATE TRIGGER update_room_members_updated_at
    BEFORE UPDATE
    ON
        room_members
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();