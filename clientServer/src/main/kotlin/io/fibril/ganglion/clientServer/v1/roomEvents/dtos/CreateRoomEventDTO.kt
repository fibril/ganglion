package io.fibril.ganglion.clientServer.v1.roomEvents.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.roomEvents.EVENT_NAME_TO_CONTENT_SCHEMA_MAP
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventSchemas
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema

data class CreateRoomEventDTO @Inject constructor(
    val json: JsonObject,
    val roomEventName: String,
    override val sender: User
) : DTO(json) {
    override val schema: JsonSchema
        get() = kotlin.run {
            val baseRoomEventSchemaJson = RoomEventSchemas.BaseRoomEventSchema.toJson()
            val contentSchema = EVENT_NAME_TO_CONTENT_SCHEMA_MAP[roomEventName]
                ?: throw IllegalArgumentException("Unknown RoomEventName")

            return JsonSchema.of(
                baseRoomEventSchemaJson.mergeIn(
                    JsonObject.of(
                        "content", contentSchema.toJson()
                    )
                )
            )
        }

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

}
