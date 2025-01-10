CREATE TABLE IF NOT EXISTS devices (
    id varchar PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    display_name varchar(255),
    user_id varchar NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX devices_user_id_idx ON devices (user_id);

CREATE TRIGGER update_devices_updated_at
    BEFORE UPDATE
    ON
        devices
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();