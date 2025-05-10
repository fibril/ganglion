CREATE TABLE IF NOT EXISTS devices (
    id VARCHAR PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    display_name VARCHAR(255),
    user_id VARCHAR NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX devices_user_id_idx ON devices (user_id);

CREATE TRIGGER update_devices_updated_at
    BEFORE UPDATE
    ON
        devices
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();