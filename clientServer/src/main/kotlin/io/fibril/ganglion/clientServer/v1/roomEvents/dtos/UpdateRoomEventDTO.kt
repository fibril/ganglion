package io.fibril.ganglion.clientServer.v1.roomEvents.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.roomEvents.EVENT_NAME_TO_CONTENT_SCHEMA_MAP
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas

data class UpdateRoomEventDTO @Inject constructor(
    private val json: JsonObject,
    val roomEventName: String,
    override val sender: User?
) : DTO(json) {
    override val schema: JsonSchema
        get() = kotlin.run {
            val contentSchema = EVENT_NAME_TO_CONTENT_SCHEMA_MAP[roomEventName]
                ?: throw IllegalArgumentException("Unknown RoomEventName")

            return JsonSchema.of(
                Schemas.objectSchema().optionalProperty("sender", MatrixUserId.createMatrixUserIdStringSchema())
                    .optionalProperty("type", Schemas.stringSchema())
                    .optionalProperty("state_key", Schemas.stringSchema())
                    .optionalProperty("room_id", RoomId.createRoomIdStringSchema())
                    .optionalProperty("content", contentSchema)
                    .toJson()
            )


        }
    override val permittedParams: Set<String> = setOf(
        "sender",
        "type",
        "state_key",
        "room_id",
        "content"
    )

    override val paramNameTransformMapping: Map<String, String> = mapOf(
        "eventType" to "type",
        "roomId" to "room_id",
        "stateKey" to "state_key",
        "txnId" to "transaction_id"
    )

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

}
