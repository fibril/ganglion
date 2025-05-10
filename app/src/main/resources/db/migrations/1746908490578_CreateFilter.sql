CREATE TABLE IF NOT EXISTS filters (
    id VARCHAR PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    user_id VARCHAR NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content JSONB NOT NULL
);

CREATE INDEX filter_user_id_idx ON filters (user_id);

CREATE TRIGGER update_filters_updated_at
    BEFORE UPDATE
    ON
        filters
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER notify_filters_resource_created
    AFTER INSERT
    ON
        filters
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_created();

CREATE TRIGGER notify_filters_resource_updated
    AFTER UPDATE
    ON
        filters
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_updated();

CREATE TRIGGER notify_filters_resource_deleted
    AFTER DELETE
    ON
        filters
    FOR EACH ROW
EXECUTE PROCEDURE notify_resource_deleted();