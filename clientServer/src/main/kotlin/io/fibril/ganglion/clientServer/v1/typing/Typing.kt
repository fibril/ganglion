package io.fibril.ganglion.clientServer.v1.typing

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject


interface TypingModel : Model

data class Typing @Inject constructor(val roomId: RoomId, val userIds: JsonArray) : TypingModel {
    companion object {
        const val TYPING_TYPE = "m.typing"
    }

    override fun asJson(): JsonObject =
        JsonObject()
            .put(
                "content", JsonObject.of("user_ids", userIds)
            )
            .put("room_id", roomId.toString())
            .put("type", TYPING_TYPE)
}
