CREATE OR REPLACE FUNCTION update_room_after_event_changes()
RETURNS TRIGGER AS $room_updater$
BEGIN
    CASE
        WHEN NEW.type = 'm.room.create' THEN
            UPDATE rooms
            SET type = (NEW.content::jsonb)->>'type'
            WHERE id = NEW.room_id;

        WHEN NEW.type = 'm.room.canonical_alias' THEN
            UPDATE rooms
            SET canonical_alias = (NEW.content::jsonb)->>'alias'
            WHERE id = NEW.room_id;

        WHEN NEW.type = 'm.room.guest_access' THEN
            UPDATE rooms
            SET guest_access = (NEW.content::jsonb)->>'guest_access'
            WHERE id = NEW.room_id;

        WHEN NEW.type = 'm.room.join_rules' THEN
            UPDATE rooms
            SET join_rule = (NEW.content::jsonb)->>'join_rule'
            WHERE id = NEW.room_id;

        WHEN NEW.type = 'm.room.name' THEN
            UPDATE rooms
            SET name = (NEW.content::jsonb)->>'name'
            WHERE id = NEW.room_id;

        WHEN NEW.type = 'm.room.topic' THEN
            UPDATE rooms
            SET topic = (NEW.content::jsonb)->>'topic'
            WHERE id = NEW.room_id;

        WHEN NEW.type = 'm.room.history_visibility' THEN
            UPDATE rooms
            SET history_visibility = (NEW.content::jsonb)->>'history_visibility'
            WHERE id = NEW.room_id;

        WHEN NEW.type = 'm.room.member' THEN
            IF (OLD.content::jsonb)->>'membership' IS DISTINCT FROM (NEW.content::jsonb)->>'membership' AND (NEW.content::jsonb)->>'membership' = 'join' THEN
                UPDATE rooms
                SET num_joined_members = num_joined_members + 1
                WHERE id = NEW.room_id;
            END IF;

            IF (OLD.content::jsonb)->>'membership' IS DISTINCT FROM (NEW.content::jsonb)->>'membership' AND (OLD.content::jsonb)->>'membership' = 'join' THEN
                UPDATE rooms
                SET num_joined_members = num_joined_members - 1
                WHERE id = NEW.room_id;
            END IF;

        ELSE

    END CASE;

    RETURN NEW;
END;
$room_updater$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_room_after_event_changes
AFTER INSERT OR UPDATE ON room_events
FOR EACH ROW
EXECUTE FUNCTION update_room_after_event_changes();