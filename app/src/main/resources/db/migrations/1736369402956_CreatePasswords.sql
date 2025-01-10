CREATE TABLE IF NOT EXISTS passwords (
    id varchar PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    user_id varchar NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    hash varchar NOT NULL,
    CONSTRAINT one_password_per_user UNIQUE (user_id)
);

CREATE INDEX passwords_user_id_idx ON passwords (user_id);

CREATE TRIGGER update_passwords_updated_at
    BEFORE UPDATE
    ON
        passwords
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();