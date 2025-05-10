CREATE TABLE IF NOT EXISTS media (
    id VARCHAR PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    user_id VARCHAR NOT NULL REFERENCES users (id),
    content_type VARCHAR(100),
    content_disposition VARCHAR(255),
    content_transfer_encoding VARCHAR,
    title VARCHAR(255),
    description TEXT,
    filename VARCHAR,
    charset VARCHAR
);

CREATE TRIGGER update_media_updated_at
    BEFORE UPDATE
    ON
        media
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE INDEX media_user_id_idx on media (user_id);