package io.fibril.ganglion.clientServer

import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.*
import io.vertx.ext.auth.User as VertxUser

data class DTOValidationResult(val valid: Boolean, val errors: JsonObject)

abstract class DTO(private val json: JsonObject) {

    /**
     * Authenticated Vertx user object.
     * This represents the user that created this DTO
     */
    abstract val sender: VertxUser?

    /**
     * Define a custom schema to validate the supplied JSON
     */
    abstract val schema: JsonSchema

    /**
     * A set of params that can be saved to the database
     */
    abstract val permittedParams: Set<String>

    /**
     * Provide an alternative mapping of supplied parameter name to desired name.
     * For example, you can map a param named userId to user_id, this will replace
     * the name of the param userId with user_id
     */
    abstract val paramNameTransformMapping: Map<String, String>

    abstract fun validate(): DTOValidationResult


    fun params(): JsonObject {
        return JsonObject().apply {
            val keys = this@DTO.json.map.keys
            for (key in keys) {
                val newKeyName = paramNameTransformMapping[key] ?: key
                if (permittedParams.contains(key)) {
                    put(newKeyName, json.getValue(key))
                }
            }
        }
    }

    object Helpers {
        fun validate(json: JsonObject, schema: JsonSchema): DTOValidationResult {
            return try {
                val validator =
                    Validator.create(
                        schema,
                        JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("https://fibril.io")
                    )
                val result = validator.validate(json)

                val errors = JsonObject().apply {
                    if (!result.valid && (result?.errors?.size ?: 0) > 0) {
                        for (err in result.errors) {
                            put(err.instanceLocation, err.error)
                        }
                        if (result.error != null) put("message", result.error)
                    }
                }
                DTOValidationResult(result.valid, errors)

            } catch (e: SchemaException) {
                println(e)
                DTOValidationResult(false, JsonObject().put("message", e.message))
            }
        }

        fun useDTOValidation(
            dto: DTO,
            context: RoutingContext,
            onValidationFailure: ((routingContext: RoutingContext, errors: JsonObject) -> Unit)? = Helpers::defaultOnValidationFailure,
            onValidationSuccess: () -> Unit
        ) {
            val validationResult = dto.validate()
            if (validationResult.valid) {
                onValidationSuccess()
            } else {
                onValidationFailure!!(context, validationResult.errors)
            }
        }

        private fun defaultOnValidationFailure(context: RoutingContext, errors: JsonObject) {
            context.response().setStatusCode(400)
            context.end(
                StandardErrorResponse(
                    ErrorCodes.M_INVALID_PARAM,
                    additionalKeyValues = JsonObject().apply { if (!errors.isEmpty) put("errors", errors) }
                ).toString()
            )
        }
    }
}