CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(510) PRIMARY KEY NOT NULL CHECK (id ~ '^@[a-zA-Z0-9_\-./]+:[a-zA-Z0-9\-._~]+$'),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    confirmed_at TIMESTAMP WITHOUT TIME ZONE,
    locked_at TIMESTAMP WITHOUT TIME ZONE,
    suspended_at TIMESTAMP WITHOUT TIME ZONE,
    role VARCHAR NOT NULL DEFAULT 'user' CHECK (role IN('admin', 'user', 'bot', 'trusted_application', 'guest'))
);

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE
    ON
        users
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();



