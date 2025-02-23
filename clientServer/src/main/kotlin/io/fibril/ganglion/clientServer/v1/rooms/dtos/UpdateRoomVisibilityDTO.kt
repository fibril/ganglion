package io.fibril.ganglion.clientServer.v1.rooms.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas

data class UpdateRoomVisibilityDTO @Inject constructor(
    private val json: JsonObject,
    override val sender: User? = null
) :
    DTO(json) {
    override val schema: JsonSchema =
        JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "roomId",
                    RoomId.createRoomIdStringSchema()
                )
                .requiredProperty(
                    "visibility",
                    Schemas.stringSchema().with(
                        Keywords.pattern(
                            Regex("""^(private|public)$""").toPattern()
                        )
                    )
                )
                .toJson()
        )

    override val permittedParams: Set<String>
        get() = setOf("visibility")

    override val paramNameTransformMapping: Map<String, String>
        get() = mapOf()

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

}

