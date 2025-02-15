package io.fibril.ganglion.clientServer.v1.rooms.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas

data class KnockRoomDTO @Inject constructor(
    private val json: JsonObject,
    override val sender: User? = null
) :
    DTO(json) {
    override val schema: JsonSchema =
        JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "roomIdOrAlias",
                    Schemas.stringSchema()
                )
                .optionalProperty("server_name", Schemas.arraySchema().items(Schemas.stringSchema()))
                .optionalProperty("via", Schemas.arraySchema().items(Schemas.stringSchema()))
                .optionalProperty("reason", Schemas.stringSchema())
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

