package io.fibril.ganglion.clientServer.v1.rooms.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class CreateRoomAliasDTO @Inject constructor(
    private val json: JsonObject,
    override val sender: VertxUser? = null
) :
    DTO(json) {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "room_id", RoomId.RoomIdStringSchema
                )
                .requiredProperty("alias", Schemas.stringSchema())
                .optionalProperty("servers", Schemas.arraySchema().with(Keywords.uniqueItems()))
                .toJson()
        )
    override val permittedParams: Set<String> = json.map.keys

    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

}

