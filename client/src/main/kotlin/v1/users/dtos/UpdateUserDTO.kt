package v1.users.dtos

import DTO
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import utils.ResourceBundleConstants

data class UpdateUserDTO(val json: JsonObject) : DTO {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    ResourceBundleConstants.matrixFields.getString("user.id"),
                    Schemas.stringSchema()
                ).toJson()
        )

    override fun validate(): Boolean {
        return DTO.Helpers.validate(json, schema)
    }
}