package io.fibril.ganglion.clientServer.v1.authentication.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.users.models.User
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class LoginDTO(private val json: JsonObject, override val sender: VertxUser? = null) : DTO(json) {
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


    override val permittedParams: Set<String> = json.map.keys

    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }
}