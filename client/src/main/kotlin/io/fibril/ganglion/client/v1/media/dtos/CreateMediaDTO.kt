package io.fibril.ganglion.client.v1.media.dtos

import io.fibril.ganglion.client.DTO
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import io.fibril.ganglion.client.v1.users.models.MatrixUserId

data class CreateMediaDTO(val json: JsonObject) : DTO {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "userId",
                    MatrixUserId.MatrixUserIdStringSchema
                )
                .toJson()
        )

    override fun validate(): Boolean {
        return DTO.Helpers.validate(json, schema)
    }

}