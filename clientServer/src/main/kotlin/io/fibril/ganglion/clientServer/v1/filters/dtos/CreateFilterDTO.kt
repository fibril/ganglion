package io.fibril.ganglion.clientServer.v1.filters.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class CreateFilterDTO(val json: JsonObject, override val sender: VertxUser?) : DTO(json) {
    private val eventFilter = Schemas.objectSchema()
        .optionalProperty("limit", Schemas.intSchema())
        .optionalProperty(
            "not_senders", Schemas.arraySchema().items(
                MatrixUserId.createMatrixUserIdStringSchema()
            )
        )
        .optionalProperty(
            "not_types", Schemas.arraySchema().items(
                Schemas.stringSchema()
            )
        )
        .optionalProperty(
            "senders", Schemas.arraySchema().items(
                MatrixUserId.createMatrixUserIdStringSchema()
            )
        )
        .optionalProperty(
            "types", Schemas.arraySchema().items(
                Schemas.stringSchema()
            )
        )


    private val roomEventFilter = Schemas.objectSchema()
        .optionalProperty("contains_url", Schemas.booleanSchema())
        .optionalProperty("include_redundant_members", Schemas.booleanSchema())
        .optionalProperty("lazy_load_members", Schemas.booleanSchema())
        .optionalProperty("limit", Schemas.intSchema())
        .optionalProperty(
            "not_rooms", Schemas.arraySchema().items(
                RoomId.createRoomIdStringSchema()
            )
        )
        .optionalProperty(
            "rooms", Schemas.arraySchema().items(
                RoomId.createRoomIdStringSchema()
            )
        )
        .optionalProperty(
            "not_senders", Schemas.arraySchema().items(
                MatrixUserId.createMatrixUserIdStringSchema()
            )
        )
        .optionalProperty(
            "not_types", Schemas.arraySchema().items(
                Schemas.stringSchema()
            )
        )
        .optionalProperty(
            "senders", Schemas.arraySchema().items(
                MatrixUserId.createMatrixUserIdStringSchema()
            )
        )
        .optionalProperty(
            "types", Schemas.arraySchema().items(
                Schemas.stringSchema()
            )
        )
        .optionalProperty("unread_thread_notifications", Schemas.booleanSchema())

    private val roomFilter = Schemas.objectSchema()
        .optionalProperty("account_data", roomEventFilter)
        .optionalProperty("ephemeral", roomEventFilter)
        .optionalProperty("include_leave", Schemas.booleanSchema())
        .optionalProperty(
            "not_rooms", Schemas.arraySchema().items(
                RoomId.createRoomIdStringSchema()
            )
        )
        .optionalProperty(
            "rooms", Schemas.arraySchema().items(
                RoomId.createRoomIdStringSchema()
            )
        )
        .optionalProperty("state", roomEventFilter)
        .optionalProperty("timeline", roomEventFilter)


    override val schema: JsonSchema = JsonSchema.of(
        Schemas.objectSchema()
            .requiredProperty("userId", MatrixUserId.createMatrixUserIdStringSchema())
            .optionalProperty("account_data", eventFilter)
            .optionalProperty("event_fields", Schemas.arraySchema().items(Schemas.stringSchema()))
            .optionalProperty(
                "event_format",
                Schemas.stringSchema().with(Keywords.pattern("""^(client|federation)$""".toPattern()))
            )
            .optionalProperty("presence", eventFilter)
            .optionalProperty("room", roomFilter)
            .toJson()
    )

    override val permittedParams: Set<String> = json.map.keys
    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }

}