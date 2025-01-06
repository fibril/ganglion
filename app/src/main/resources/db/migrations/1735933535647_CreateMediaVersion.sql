CREATE TABLE IF NOT EXISTS media_versions (
    id varchar PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    name varchar(20) NOT NULL DEFAULT 'original',
    file_data BYTEA NOT NULL,
    height smallint,
    width  smallint,
    animated BOOLEAN NOT NULL DEFAULT FALSE,

    media_id varchar NOT NULL REFERENCES media (id) ON DELETE CASCADE,

    CONSTRAINT check_name_permitted CHECK(name IN(
        'original',
        'crop32x32',
        'crop96x96',
        'scale320x240',
        'scale640x480',
        'scale800x600'
        )),
    CONSTRAINT one_media_version_name_per_media UNIQUE (media_id, name)
);

CREATE TRIGGER update_media_versions_updated_at
    BEFORE UPDATE
    ON
        media_versions
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE INDEX media_versions_media_id_idx ON media_versions (media_id);