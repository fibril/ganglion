CREATE TABLE IF NOT EXISTS user_profiles (
    id varchar PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    user_id varchar NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    avatar_id varchar REFERENCES media (id),
    display_name varchar(255),
    CONSTRAINT one_user_profile_per_user UNIQUE (user_id)
);

CREATE INDEX user_profiles_user_id_idx ON user_profiles (user_id);
CREATE INDEX user_profiles_avatar_id_idx ON user_profiles (avatar_id);

CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE
    ON
        user_profiles
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();


