import errors.ErrorCodes
import errors.StandardErrorResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.*


interface DTO {

    /**
     * Define a custom schema to validate the supplied JSON
     */
    val schema: JsonSchema

    fun validate(): Boolean

    object Helpers {
        fun validate(json: JsonObject, schema: JsonSchema): Boolean {
            return try {
                val validator =
                    Validator.create(
                        schema,
                        JsonSchemaOptions().setDraft(Draft.DRAFT7).setBaseUri("https://vertx.io")
                    )
                val result = validator.validate(json)

                result.valid

            } catch (e: SchemaException) {
                println(e)
                false
            }
        }

        fun useDTOValidation(
            dto: DTO,
            context: RoutingContext,
            onValidationFailure: ((routingContext: RoutingContext) -> Unit)? = ::defaultOnValidationFailure,
            onValidationSuccess: () -> Unit
        ) {
            if (dto.validate()) {
                onValidationSuccess()
            } else {
                onValidationFailure!!(context)
            }
        }

        private fun defaultOnValidationFailure(context: RoutingContext) {
            context.response().setStatusCode(400)
            context.end(
                StandardErrorResponse(
                    ErrorCodes.M_INVALID_PARAM,
                ).toString()
            )
        }
    }
}