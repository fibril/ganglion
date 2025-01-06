import io.vertx.core.json.JsonObject

interface Model {

    fun asJson(permittedFields: List<String>?): JsonObject

}