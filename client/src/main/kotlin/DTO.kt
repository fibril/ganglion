import io.vertx.core.json.JsonObject
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

        fun useDTOValidation(dto: DTO, onValidationFailure: () -> Unit, block: () -> Unit) {
            if (dto.validate()) {
                block()
            } else {
                onValidationFailure()
            }
        }
    }
}