package io.fibril.ganglion.clientServer.v1.media.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class PutMediaDTO(val json: JsonObject, override val sender: VertxUser?) : DTO(json) {
    override val schema: JsonSchema = JsonSchema.of(
        Schemas.objectSchema()
            .optionalProperty("title", Schemas.stringSchema().with(Keywords.maxLength(255)))
            .optionalProperty("description", Schemas.stringSchema())
            .requiredProperty("mediaId", Schemas.stringSchema())
            .requiredProperty("serverName", Schemas.stringSchema())
            .toJson()
    )

    override val permittedParams: Set<String> = json.map.keys
    override val paramNameTransformMapping: Map<String, String> = mapOf("Content-Type" to "content_type")

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }

}