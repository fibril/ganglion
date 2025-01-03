import io.vertx.core.json.JsonObject
import io.vertx.json.schema.*

abstract class DTO {

    /**
     * Define a custom schema to validate the supplied JSON
     */
    abstract val schema: JsonSchema

    abstract fun validate(): Boolean

    companion object {
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
    }
}