package io.fibril.ganglion.clientServer.v1.users.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.vertx.core.json.JsonObject

interface UserProfileModel : Model {

}

data class UserProfile @Inject constructor(
    val id: String,
    val fullJsonObject: JsonObject? = null
) : UserProfileModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())

    companion object {
        val PROFILE_DISPLAY_NAME_KEY = ResourceBundleConstants.matrixFields.getString("profile.displayname")
        val PROFILE_AVATAR_URL_KEY = ResourceBundleConstants.matrixFields.getString("profile.avatarUrl")

    }

}
