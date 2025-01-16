CREATE TABLE IF NOT EXISTS issued_tokens (
    id varchar PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    user_id varchar NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    token_type varchar(10) NOT NULL DEFAULT 'access',
    CONSTRAINT permitted_token_type CHECK(token_type IN('access', 'refresh'))
);

CREATE INDEX issued_tokens_user_id_idx ON issued_tokens (user_id);

CREATE TRIGGER update_issued_tokens_updated_at
    BEFORE UPDATE
    ON
        issued_tokens
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();