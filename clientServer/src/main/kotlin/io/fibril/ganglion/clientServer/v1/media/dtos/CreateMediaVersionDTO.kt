package io.fibril.ganglion.clientServer.v1.media.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.media.models.MediaVersion
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class CreateMediaVersionDTO(val json: JsonObject, override val sender: VertxUser?) : DTO(json) {
    override val schema: JsonSchema = JsonSchema.of(
        Schemas.objectSchema()
            .requiredProperty("media_id", Schemas.stringSchema())
            .requiredProperty("uploaded_filename", Schemas.stringSchema())
            .requiredProperty(
                "name",
                Schemas.stringSchema().with(Keywords.pattern(MediaVersion.versionNameRegex.toPattern()))
            )
            .optionalProperty("height", Schemas.intSchema())
            .optionalProperty("width", Schemas.intSchema())
            .optionalProperty("animated", Schemas.booleanSchema())
            .optionalProperty("file_size", Schemas.intSchema())
            .toJson()
    )

    override val permittedParams: Set<String> = json.map.keys
    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }

}