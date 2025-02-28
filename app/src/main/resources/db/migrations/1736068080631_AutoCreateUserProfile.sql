CREATE OR REPLACE FUNCTION create_user_profile() RETURNS TRIGGER AS $cup$
    BEGIN
       INSERT INTO user_profiles (user_id, display_name) VALUES (NEW.id, substring(NEW.id FROM '@([a-zA-Z0-9_\-./]+):'));
       return NEW;
    END;
$cup$ language plpgsql;

CREATE TRIGGER create_user_profile_after_create_user
AFTER INSERT
ON users
FOR EACH ROW EXECUTE FUNCTION create_user_profile();