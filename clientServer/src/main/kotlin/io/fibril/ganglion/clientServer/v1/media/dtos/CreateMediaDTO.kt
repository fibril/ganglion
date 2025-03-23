package io.fibril.ganglion.clientServer.v1.media.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class CreateMediaDTO(val json: JsonObject, override val sender: VertxUser?) : DTO(json) {
    override val schema: JsonSchema = JsonSchema.of(
        Schemas.objectSchema()
            .optionalProperty("Content-Type", Schemas.stringSchema())
            .optionalProperty("filename", Schemas.stringSchema())
            .optionalProperty("content_type", Schemas.stringSchema())
            .optionalProperty("content_disposition", Schemas.stringSchema())
            .optionalProperty("charset", Schemas.stringSchema())
            .optionalProperty("content_transfer_encoding", Schemas.stringSchema())
            .optionalProperty("title", Schemas.stringSchema())
            .optionalProperty("description", Schemas.stringSchema())
            .toJson()
    )

    override val permittedParams: Set<String> = json.map.keys
    override val paramNameTransformMapping: Map<String, String> = mapOf("Content-Type" to "content_type")

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }

}