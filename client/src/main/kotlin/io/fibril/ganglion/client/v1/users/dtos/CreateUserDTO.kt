package io.fibril.ganglion.client.v1.users.dtos

import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.v1.users.models.User
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas

data class CreateUserDTO(val json: JsonObject) : DTO {
    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(
                    "username",
                    Schemas.stringSchema().with(Keywords.pattern(User.UsernameRegex.toPattern()))
                )
                .requiredProperty(
                    "password",
                    Schemas.stringSchema().with(Keywords.pattern(User.PasswordRegex.toPattern()))
                )
                .optionalProperty("device_id", Schemas.stringSchema())
                .optionalProperty("inhibit_login", Schemas.booleanSchema())
                .optionalProperty("initial_device_display_name", Schemas.stringSchema())
                .optionalProperty("refresh_token", Schemas.booleanSchema())
                .toJson()
        )

    override fun validate(): Boolean {
        return DTO.Helpers.validate(json, schema)
    }


    companion object {
    }

}
