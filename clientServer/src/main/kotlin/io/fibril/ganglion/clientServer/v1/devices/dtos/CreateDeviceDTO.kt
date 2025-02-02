package io.fibril.ganglion.clientServer.v1.devices.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas

import io.vertx.ext.auth.User as VertxUser

data class CreateDeviceDTO constructor(val json: JsonObject, override val sender: VertxUser? = null) : DTO(json) {
    internal constructor(routingContext: RoutingContext) : this(JsonObject.mapFrom(routingContext.pathParams()))

    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "user_id",
                    MatrixUserId.createMatrixUserIdStringSchema()
                )
                .optionalProperty("display_name", Schemas.stringSchema())
                .optionalProperty("device_id", Schemas.stringSchema())
                .toJson()
        )

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }

}

