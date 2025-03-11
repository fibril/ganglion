package io.fibril.ganglion.clientServer.v1.presence

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import java.util.*


data class Presence @Inject constructor(val userId: MatrixUserId, val contentJson: JsonObject) : Model {
    constructor(fullJsonObject: JsonObject) : this(
        MatrixUserId(fullJsonObject.getString("sender")),
        fullJsonObject.getJsonObject("content")
    )

    companion object {
        const val PRESENCE_TYPE = "m.presence"
    }

    override fun asJson(): JsonObject =
        JsonObject()
            .put(
                "content", contentJson.copy().apply {
                    val currentlyActive = getString("presence") == "online"
                    put("currently_active", currentlyActive)
                    if (!currentlyActive) {
                        val lastActiveAgo = Date().time - (getInteger("updated_at") ?: 0)
                        put("last_active_ago", lastActiveAgo)
                    }
                }
            )
            .put("sender", userId.toString())
            .put("type", PRESENCE_TYPE)
}
