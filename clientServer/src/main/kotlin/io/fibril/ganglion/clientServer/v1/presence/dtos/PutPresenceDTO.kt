package io.fibril.ganglion.clientServer.v1.presence.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class PutPresenceDTO(private val json: JsonObject, override val sender: VertxUser? = null) : DTO(json) {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "presence",
                    Schemas.stringSchema()
                        .with(Keywords.pattern(Regex("""^(online|offline|unavailable)$""").toPattern()))
                )
                .requiredProperty(
                    "userId",
                    MatrixUserId.createMatrixUserIdStringSchema()
                )
                .optionalProperty("status_msg", Schemas.stringSchema().with(Keywords.maxLength(255)))
                .toJson()
        )


    override val permittedParams: Set<String> = json.map.keys

    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }


    companion object {
    }

}
