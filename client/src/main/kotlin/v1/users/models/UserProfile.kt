package v1.users.models

import Model
import io.vertx.core.json.JsonObject
import utils.ResourceBundleConstants

interface UserProfileModel : Model {

}

data class UserProfile(
    var displayName: String? = null,
    var avatarUrl: String? = null,
    val additionalKeyValues: JsonObject = JsonObject()
) : UserProfileModel {
    override fun asJson(): JsonObject =
        JsonObject()
            .put(PROFILE_DISPLAY_NAME_KEY, displayName)
            .put(PROFILE_AVATAR_URL_KEY, avatarUrl)
            .mergeIn(additionalKeyValues)

    companion object {
        val PROFILE_DISPLAY_NAME_KEY = ResourceBundleConstants.matrixFields.getString("profile.displayname")
        val PROFILE_AVATAR_URL_KEY = ResourceBundleConstants.matrixFields.getString("profile.avatarUrl")

        fun fromJson(jsonObject: JsonObject): UserProfile {
            val copyJson = jsonObject.copy()
            val displayName = jsonObject.getString(PROFILE_DISPLAY_NAME_KEY) ?: try {
                MatrixUserId(copyJson.getString("user_id")).localPart
            } catch (e: Exception) {
                null
            }
            return UserProfile(
                displayName = displayName,
                avatarUrl = jsonObject.getString(
                    PROFILE_AVATAR_URL_KEY
                ),
                additionalKeyValues = copyJson.apply {
                    this.remove(PROFILE_DISPLAY_NAME_KEY)
                    this.remove(PROFILE_AVATAR_URL_KEY)
                }
            )
        }
    }

}
