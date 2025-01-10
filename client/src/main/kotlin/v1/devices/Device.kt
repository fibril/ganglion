package v1.devices

import Model
import com.google.inject.Inject
import io.vertx.core.json.JsonObject

data class Device @Inject constructor(val deviceId: String, val userId: String, val deviceName: String) : Model {

    override fun asJson(): JsonObject =
        JsonObject().put("id", deviceId).put("name", deviceName).put("user_id", userId)
}
