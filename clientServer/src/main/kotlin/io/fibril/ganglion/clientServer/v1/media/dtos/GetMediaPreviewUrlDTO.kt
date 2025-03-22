package io.fibril.ganglion.clientServer.v1.media.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.media.models.MediaUri
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class GetMediaPreviewUrlDTO(val json: JsonObject, override val sender: VertxUser) : DTO(json) {
    override val schema: JsonSchema = JsonSchema.of(
        Schemas.objectSchema()
            .requiredProperty("url", MediaUri.createMediaUriStringSchema())
            .optionalProperty("ts", Schemas.intSchema())
            .toJson()
    )

    override val permittedParams: Set<String> = json.map.keys
    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }

}