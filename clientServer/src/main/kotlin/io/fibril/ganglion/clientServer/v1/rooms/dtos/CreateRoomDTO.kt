package io.fibril.ganglion.clientServer.v1.rooms.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventSchemas
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class CreateRoomDTO @Inject constructor(private val json: JsonObject, override val sender: VertxUser? = null) :
    DTO(json) {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .optionalProperty(
                    "creation_content", Schemas.arraySchema().items(
                        RoomEventSchemas.ContentSchemas.CREATE_ROOM_CONTENT_SCHEMA_BUILDER
                    )
                )
                .optionalProperty(
                    "initial_state", Schemas.arraySchema().items(
                        Schemas.objectSchema()
                            .requiredProperty("content", Schemas.objectSchema())
                            .requiredProperty("type", Schemas.stringSchema())
                            .optionalProperty("state_key", Schemas.stringSchema())
                    )
                )
                .optionalProperty(
                    "invite",
                    Schemas.arraySchema().items(MatrixUserId.createMatrixUserIdStringSchema())
                )
                .optionalProperty(
                    "invite_3pid", Schemas.arraySchema().items(
                        Schemas.objectSchema()
                            .requiredProperty("address", Schemas.stringSchema())
                            .requiredProperty("id_access_token", Schemas.stringSchema())
                            .requiredProperty("id_server", Schemas.stringSchema())
                            .requiredProperty("medium", Schemas.stringSchema())
                    )
                )
                .optionalProperty("is_direct", Schemas.booleanSchema())
                .optionalProperty("name", Schemas.stringSchema())
                .optionalProperty(
                    "power_level_content_override",
                    RoomEventSchemas.ContentSchemas.POWER_LEVELS_CONTENT_SCHEMA_BUILDER
                )
                .optionalProperty("preset", Schemas.stringSchema())
                .optionalProperty("room_alias_name", Schemas.stringSchema())
                .optionalProperty("room_version", Schemas.stringSchema())
                .optionalProperty("topic", Schemas.stringSchema())
                .optionalProperty("visibility", Schemas.stringSchema())
                .toJson()
        )
    override val permittedParams: Set<String> = json.map.keys

    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

}
