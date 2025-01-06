CREATE OR REPLACE FUNCTION create_user_profile() RETURNS TRIGGER AS $cup$
    BEGIN
       INSERT INTO user_profiles (user_id) VALUES (NEW.id);
       return NEW;
    END;
$cup$ language plpgsql;

CREATE TRIGGER create_user_profile_after_create_user
AFTER INSERT
ON users
FOR EACH ROW EXECUTE FUNCTION create_user_profile();