package io.fibril.ganglion.clientServer.v1.presence.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class GetPresenceDTO(private val json: JsonObject, override val sender: VertxUser? = null) : DTO(json) {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "userId",
                    MatrixUserId.createMatrixUserIdStringSchema()
                )
                .toJson()
        )

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }


    companion object {
    }

}
