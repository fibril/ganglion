package io.fibril.ganglion.clientServer.v1.roomEvents.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject

interface RoomEventModel : Model

data class RoomEvent @Inject constructor(val id: String, val fullJsonObject: JsonObject) : RoomEventModel {
    internal constructor(jsonObject: JsonObject) : this(jsonObject.getString("id"), jsonObject)

    override fun asJson(): JsonObject = JsonObject.of(
        "id", id,
        "event_id", id
    ).mergeIn(fullJsonObject)


}
