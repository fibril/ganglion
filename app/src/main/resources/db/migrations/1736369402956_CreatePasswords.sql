CREATE TABLE IF NOT EXISTS passwords (
    id VARCHAR PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    user_id VARCHAR NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    hash VARCHAR NOT NULL,
    CONSTRAINT one_password_per_user UNIQUE (user_id)
);

CREATE INDEX passwords_user_id_idx ON passwords (user_id);

CREATE TRIGGER update_passwords_updated_at
    BEFORE UPDATE
    ON
        passwords
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();