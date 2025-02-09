package io.fibril.ganglion.clientServer.v1.users.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject

interface UserProfileModel : Model {

}

data class UserProfile @Inject constructor(
    val id: String,
    val fullJsonObject: JsonObject? = null
) : UserProfileModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson(): JsonObject = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())
        .apply {
            val userId = MatrixUserId(getString("user_id"))
            val displayName = getString("display_name") ?: userId.localPart
            put("display_name", displayName)
            put("displayname", displayName)
        }

}
