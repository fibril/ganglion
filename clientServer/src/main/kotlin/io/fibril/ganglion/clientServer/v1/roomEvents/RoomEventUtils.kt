package io.fibril.ganglion.clientServer.v1.roomEvents

import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.vertx.core.json.JsonObject

object RoomEventUtils {

    fun generateEventId() = Utils.generateRoomEventId()

    fun generateDefaultPowerLevelsEvent(
        eventKeyValueOverrideJson: JsonObject? = JsonObject(),
        contentKeyValueOverrideJson: JsonObject? = JsonObject()
    ) = RoomEvent(
        generateEventId(),
        JsonObject()
            .put(
                "content", JsonObject()
                    .put("ban", 50)
                    .put("events_default", 0)
                    .put("invite", 0)
                    .put("kick", 50)
                    .put("notifications", JsonObject.of("room", 50))
                    .put("redact", 50)
                    .put("state_default", 50)
                    .put("users_default", 0)
                    .mergeIn(contentKeyValueOverrideJson)
            )
            .put("type", RoomEventNames.StateEvents.POWER_LEVELS)
            .put("state_key", EVENT_ONE_OF_EACH_STATE_KEY)
            .mergeIn(eventKeyValueOverrideJson)
    )

    /**
     * The unique state_key for every event that only one of
     * can exist for each room
     */
    const val EVENT_ONE_OF_EACH_STATE_KEY = ""
}