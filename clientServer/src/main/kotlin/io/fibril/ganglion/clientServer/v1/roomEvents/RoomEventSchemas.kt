package io.fibril.ganglion.clientServer.v1.roomEvents

import io.fibril.ganglion.clientServer.v1.media.models.MediaUri
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEventId
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAliasId
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder
import io.vertx.json.schema.common.dsl.SchemaBuilder
import io.vertx.json.schema.common.dsl.Schemas

object RoomEventSchemas {
    val BaseRoomEventSchema = Schemas.objectSchema()
        .optionalProperty("sender", MatrixUserId.createMatrixUserIdStringSchema())
        .requiredProperty("type", Schemas.stringSchema())
        .optionalProperty("state_key", Schemas.stringSchema())
        .optionalProperty("room_id", RoomId.createRoomIdStringSchema())
        .optionalProperty("transaction_id", Schemas.stringSchema())

        // eventType is alias for type:- Mapped to type when we call params on the dto
        .optionalProperty("eventType", Schemas.stringSchema())
        // roomId is alias for room_id:- Mapped to room_id when we call params on the dto
        .optionalProperty("roomId", RoomId.createRoomIdStringSchema())
        // stateKey is alias for state_key:- Mapped to state_key when we call params on the dto
        .optionalProperty("stateKey", Schemas.stringSchema())
        //txnId is alias for transaction_id:- Mapped to transaction_id when we call params on the dto
        .optionalProperty("txnId", Schemas.stringSchema())


    object ContentSchemas {
        val POWER_LEVELS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .optionalProperty("ban", Schemas.intSchema())
                .optionalProperty("events", Schemas.objectSchema().additionalProperties(Schemas.intSchema()))
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
                .optionalProperty(
                    "users", Schemas.objectSchema()
                        .patternProperty(MatrixUserId.MatrixUserIdStringRegex.toPattern(), Schemas.intSchema())
                        .allowAdditionalProperties(false)
                )
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
                .requiredProperty(
                    "msgtype", Schemas.stringSchema().with(
                        Keywords.pattern(
                            Regex(
                                """^("m.text"|"m.emote"|"m.notice"|"m.image"|"m.file"|"m.audio"|"m.video"|"m.location"|"m.key.verification.request")$"""
                            ).toPattern()
                        )
                    )
                )
                .requiredProperty(
                    "body", Schemas.stringSchema()
                )
                .optionalProperty("format", Schemas.stringSchema())
                .optionalProperty("formatted_body", Schemas.stringSchema())
                .optionalProperty("file", Schemas.stringSchema())
                .optionalProperty("filename", Schemas.stringSchema())
                .optionalProperty("url", MediaUri.createMediaUriStringSchema())
                .optionalProperty("geo_uri", Schemas.stringSchema())
                .optionalProperty(
                    "info", Schemas.objectSchema()
                        .optionalProperty("h", Schemas.intSchema())
                        .optionalProperty("w", Schemas.intSchema())
                        .optionalProperty("mimetype", Schemas.stringSchema())
                        .optionalProperty("size", Schemas.intSchema())
                        .optionalProperty("thumbnail_url", MediaUri.createMediaUriStringSchema())
                        .optionalProperty("thumbnail_file", Schemas.stringSchema())
                        .optionalProperty(
                            "thumbnail_info", Schemas.objectSchema()
                                .optionalProperty("h", Schemas.intSchema())
                                .optionalProperty("w", Schemas.intSchema())
                                .optionalProperty("mimetype", Schemas.stringSchema())
                                .optionalProperty("size", Schemas.intSchema())
                        )
                        // AUDIO AND VIDEO INFO
                        .optionalProperty("duration", Schemas.intSchema())

                )

        val ALIASES_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val AVATAR_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .optionalProperty("url", MediaUri.createMediaUriStringSchema())
                .optionalProperty(
                    "info", Schemas.objectSchema()
                        .optionalProperty("h", Schemas.intSchema())
                        .optionalProperty("w", Schemas.intSchema())
                        .optionalProperty("mimetype", Schemas.stringSchema())
                        .optionalProperty("size", Schemas.intSchema())
                        .optionalProperty("thumbnail_url", MediaUri.createMediaUriStringSchema())
                        .optionalProperty(
                            "thumbnail_info", Schemas.objectSchema()
                                .optionalProperty("h", Schemas.intSchema())
                                .optionalProperty("w", Schemas.intSchema())
                                .optionalProperty("mimetype", Schemas.stringSchema())
                                .optionalProperty("size", Schemas.intSchema())
                        )
                )

        val CANONICAL_ALIAS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .requiredProperty("alias", RoomAliasId.createRoomAliasIdStringSchema())
                .optionalProperty(
                    "alt_aliases",
                    Schemas.arraySchema().items(RoomAliasId.createRoomAliasIdStringSchema())
                )

        val ENCRYPTION_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val GUEST_ACCESS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema().requiredProperty(
                "guest_access", Schemas.stringSchema().with(
                    Keywords.pattern(Regex("""^(can_join|forbidden)$""").toPattern())
                )
            )

        val HISTORY_VISIBILITY_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema().requiredProperty(
                "history_visibility", Schemas.stringSchema().with(
                    Keywords.pattern(Regex("""^(invited|joined|shared|world_readable)$""").toPattern())
                )
            )

        val JOIN_RULES_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .requiredProperty(
                    "join_rule", Schemas.stringSchema().with(
                        Keywords.pattern(
                            Regex("""^(public|knock|invite|private|restricted|knock_restricted)$""").toPattern()
                        )
                    )
                )
                .optionalProperty(
                    "allow",
                    Schemas.arraySchema()
                        .items(
                            Schemas.objectSchema()
                                .requiredProperty("room_id", RoomId.createRoomIdStringSchema())
                                .requiredProperty(
                                    "type", Schemas.stringSchema().with(
                                        Keywords.pattern(
                                            Regex("""^(m.room_membership)$""").toPattern()
                                        )
                                    )
                                )
                        )
                )

        val MEMBER_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .requiredProperty(
                    "membership", Schemas.stringSchema().with(
                        Keywords.pattern(
                            Regex("""^(invite|join|knock|leave|ban)$""").toPattern()
                        )
                    )
                )
                .optionalProperty("avatar_url", MediaUri.createMediaUriStringSchema())
                .optionalProperty("displayname", Schemas.stringSchema())
                .optionalProperty("display_name", Schemas.stringSchema())
                .optionalProperty("is_direct", Schemas.booleanSchema())
                .optionalProperty("join_authorised_via_users_server", Schemas.stringSchema())
                .optionalProperty("reason", Schemas.stringSchema())
                .optionalProperty(
                    "third_party_invite",
                    Schemas.objectSchema()
                        .requiredProperty("display_name", Schemas.stringSchema())
                        .requiredProperty(
                            "signed", Schemas.objectSchema()
                                .requiredProperty("mxid", MatrixUserId.createMatrixUserIdStringSchema())
                                .requiredProperty("token", Schemas.stringSchema())
                                .requiredProperty("signatures", Schemas.objectSchema())
                        )
                )

        val NAME_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema().requiredProperty("name", Schemas.stringSchema())

        val PINNED_EVENTS_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()
                .requiredProperty("pinned", Schemas.arraySchema().items(RoomEventId.createRoomEventIdStringSchema()))

        val SERVER_ACL_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val THIRD_PARTY_INVITE_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema()

        val TOPIC_CONTENT_SCHEMA_BUILDER: ObjectSchemaBuilder =
            Schemas.objectSchema().requiredProperty("topic", Schemas.stringSchema())
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