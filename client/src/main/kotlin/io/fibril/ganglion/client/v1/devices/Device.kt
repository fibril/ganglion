package io.fibril.ganglion.client.v1.devices

import com.google.inject.Inject
import io.fibril.ganglion.client.Model
import io.vertx.core.json.JsonObject

interface DeviceModel : Model

data class Device @Inject constructor(val deviceId: String, val userId: String, val deviceName: String) : DeviceModel {

    override fun asJson(): JsonObject =
        JsonObject().put("id", deviceId).put("name", deviceName).put("user_id", userId)

    companion object {
        fun fromJson(json: JsonObject): Device {
            return Device(json.getString("id"), json.getString("user_id"), json.getString("display_name"))
        }
    }
}
