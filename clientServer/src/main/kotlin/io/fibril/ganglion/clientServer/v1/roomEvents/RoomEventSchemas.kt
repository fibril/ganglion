package io.fibril.ganglion.clientServer.v1.roomEvents

import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder
import io.vertx.json.schema.common.dsl.SchemaBuilder
import io.vertx.json.schema.common.dsl.Schemas

object RoomEventSchemas {
    val BaseRoomEventSchema = Schemas.objectSchema()
        .optionalProperty("sender", MatrixUserId.createMatrixUserIdStringSchema())
        .requiredProperty("type", Schemas.stringSchema())
        .optionalProperty("state_key", Schemas.stringSchema())
        .optionalProperty("room_id", RoomId.RoomIdStringSchema)


    object ContentSchemas {
        val POWER_LEVELS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .optionalProperty("ban", Schemas.intSchema())
                .optionalProperty("events", Schemas.objectSchema())
                .optionalProperty("events_default", Schemas.intSchema())
                .optionalProperty("invite", Schemas.intSchema())
                .optionalProperty("kick", Schemas.intSchema())
                .optionalProperty(
                    "notifications", Schemas.objectSchema().requiredProperty(
                        "room", Schemas.intSchema()
                    )
                )
                .optionalProperty("redact", Schemas.intSchema())
                .optionalProperty("state_default", Schemas.intSchema())
                .optionalProperty("users", Schemas.objectSchema())
                .optionalProperty("users_default", Schemas.intSchema())

        val CREATE_ROOM_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .optionalProperty("creator", MatrixUserId.createMatrixUserIdStringSchema())
                .optionalProperty("sender", MatrixUserId.createMatrixUserIdStringSchema())
                .optionalProperty(
                    "predecessor", Schemas.objectSchema()
                        .requiredProperty("event_id", Schemas.stringSchema())
                        .requiredProperty("room_id", Schemas.stringSchema())
                )
                .optionalProperty("m.federate", Schemas.booleanSchema())
                .optionalProperty("room_version", Schemas.stringSchema())
                .optionalProperty("type", Schemas.stringSchema())

        val MESSAGE_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val ALIASES_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val AVATAR_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val CANONICAL_ALIAS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val ENCRYPTION_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val GUEST_ACCESS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val HISTORY_VISIBILITY_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val JOIN_RULES_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val MEMBER_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val NAME_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val PINNED_EVENTS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val SERVER_ACL_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val THIRD_PARTY_INVITE_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val TOPIC_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
    }
}


val EVENT_NAME_TO_CONTENT_SCHEMA_MAP: Map<String, SchemaBuilder<*, *>> = mapOf(
    //
    RoomEventNames.MESSAGE to RoomEventSchemas.ContentSchemas.MESSAGE_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.ALIASES to RoomEventSchemas.ContentSchemas.ALIASES_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.AVATAR to RoomEventSchemas.ContentSchemas.AVATAR_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.CANONICAL_ALIAS to RoomEventSchemas.ContentSchemas.CANONICAL_ALIAS_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.CREATE to RoomEventSchemas.ContentSchemas.CREATE_ROOM_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.ENCRYPTION to RoomEventSchemas.ContentSchemas.ENCRYPTION_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.GUEST_ACCESS to RoomEventSchemas.ContentSchemas.GUEST_ACCESS_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.HISTORY_VISIBILITY to RoomEventSchemas.ContentSchemas.HISTORY_VISIBILITY_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.JOIN_RULES to RoomEventSchemas.ContentSchemas.JOIN_RULES_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.MEMBER to RoomEventSchemas.ContentSchemas.MEMBER_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.NAME to RoomEventSchemas.ContentSchemas.NAME_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.PINNED_EVENTS to RoomEventSchemas.ContentSchemas.PINNED_EVENTS_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.POWER_LEVELS to RoomEventSchemas.ContentSchemas.POWER_LEVELS_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.SERVER_ACL to RoomEventSchemas.ContentSchemas.SERVER_ACL_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.THIRD_PARTY_INVITE to RoomEventSchemas.ContentSchemas.THIRD_PARTY_INVITE_CONTENT_SCHEMA_BUILDER,
    RoomEventNames.StateEvents.TOPIC to RoomEventSchemas.ContentSchemas.TOPIC_CONTENT_SCHEMA_BUILDER,
)