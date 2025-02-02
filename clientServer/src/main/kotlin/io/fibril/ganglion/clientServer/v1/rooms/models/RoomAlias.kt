package io.fibril.ganglion.clientServer.v1.rooms.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject

interface RoomAliasModel : Model

data class RoomAlias @Inject constructor(val id: String, val fullJsonObject: JsonObject? = null) : RoomAliasModel {
    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())
}
