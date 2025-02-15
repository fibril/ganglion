package io.fibril.ganglion.clientServer.v1.rooms.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import java.util.regex.Pattern

data class JoinRoomViaRoomIdDTO @Inject constructor(
    private val json: JsonObject,
    override val sender: User? = null
) :
    DTO(json) {
    override val schema: JsonSchema =
        JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty("roomId", RoomId.createRoomIdStringSchema())
                .optionalProperty("reason", Schemas.stringSchema())
                .optionalProperty(
                    "third_party_signed",
                    Schemas.objectSchema()
                        .requiredProperty("mxid", Schemas.stringSchema())
                        .requiredProperty("sender", MatrixUserId.createMatrixUserIdStringSchema())
                        .requiredProperty(
                            "signatures", Schemas.objectSchema() // {string: {string: string}}
                                .patternProperty(
                                    Pattern.compile(".*"), Schemas.objectSchema()
                                        .patternProperty(Pattern.compile(".*"), Schemas.stringSchema())
                                )
                                .allowAdditionalProperties(false)
                        )
                        .requiredProperty("token", Schemas.stringSchema())
                )
                .toJson()
        )

    override val permittedParams: Set<String>
        get() = json.map.keys

    override val paramNameTransformMapping: Map<String, String>
        get() = mapOf()

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

}

