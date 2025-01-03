import io.vertx.core.json.JsonObject
import java.util.*

abstract class Model : DTO() {

    abstract fun asJson(): JsonObject

    companion object {
        val matrixFields = ResourceBundle.getBundle("matrixFields")
    }
}