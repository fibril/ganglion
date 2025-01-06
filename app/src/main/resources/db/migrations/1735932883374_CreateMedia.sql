CREATE TABLE IF NOT EXISTS media (
    id varchar PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    user_id varchar NOT NULL REFERENCES users (id),
    content_type varchar(100),
    content_disposition varchar(255),
    preview_url varchar,
    media_type varchar(100) NOT NULL,
    title varchar(255),
    description TEXT
);

CREATE TRIGGER update_media_updated_at
    BEFORE UPDATE
    ON
        media
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE INDEX media_user_id_idx on media (user_id);