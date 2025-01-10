import io.vertx.core.json.JsonObject

interface Model {

    fun asJson(): JsonObject


}