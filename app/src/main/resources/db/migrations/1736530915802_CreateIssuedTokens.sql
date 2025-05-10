CREATE TABLE IF NOT EXISTS issued_tokens (
    id VARCHAR PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
    user_id VARCHAR NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    device_id VARCHAR NOT NULL REFERENCES devices (id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    token_type VARCHAR(10) NOT NULL DEFAULT 'access',
    CONSTRAINT permitted_token_type CHECK(token_type IN('access', 'refresh'))
);

CREATE INDEX issued_tokens_user_id_idx ON issued_tokens (user_id);
CREATE INDEX issued_tokens_token_type_token_idx ON issued_tokens (token, token_type);
CREATE INDEX issued_tokens_device_id_idx ON issued_tokens (device_id);

CREATE TRIGGER update_issued_tokens_updated_at
    BEFORE UPDATE
    ON
        issued_tokens
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();