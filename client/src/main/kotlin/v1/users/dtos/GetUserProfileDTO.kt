package v1.users.dtos

import DTO
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas
import v1.users.models.MatrixUserId

data class GetUserProfileDTO constructor(val json: JsonObject) : DTO {
    internal constructor(routingContext: RoutingContext) : this(JsonObject.mapFrom(routingContext.pathParams()))

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
