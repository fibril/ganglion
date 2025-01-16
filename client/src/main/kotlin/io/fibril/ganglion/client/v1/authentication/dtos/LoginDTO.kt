package io.fibril.ganglion.client.v1.authentication.dtos

import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.v1.users.models.User
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas

data class LoginDTO(val json: JsonObject) : DTO {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "identifier",
                    Schemas.objectSchema()
                        .requiredProperty("type", Schemas.stringSchema())
                        .requiredProperty(
                            "user", Schemas.stringSchema().with(Keywords.pattern(User.UsernameRegex.toPattern()))
                        )
                )
                .requiredProperty(
                    "password",
                    Schemas.stringSchema()
                )
                .requiredProperty("type", Schemas.stringSchema())
                .optionalProperty("device_id", Schemas.stringSchema())
                .optionalProperty("initial_device_display_name", Schemas.stringSchema())
                .optionalProperty("refresh_token", Schemas.booleanSchema())
                .toJson()
        )

    override fun validate(): Boolean {
        return DTO.Helpers.validate(json, schema)
    }
}