package io.fibril.ganglion.clientServer.v1.devices

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject

interface DeviceModel : Model

data class Device @Inject constructor(
    val id: String, val fullJsonObject: JsonObject? = null
) : DeviceModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())

    companion object {
       
    }
}
