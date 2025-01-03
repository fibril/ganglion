package v1.users.models

import DTO
import Model
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas

data class UserProfile(
    var displayName: String? = null,
    var avatarUrl: String? = null,
    val additionalKeyValues: Map<String, Any> = mapOf()
) : Model() {
    override fun asJson(): JsonObject =
        JsonObject()
            .put(PROFILE_DISPLAY_NAME_KEY, displayName)
            .put(PROFILE_AVATAR_URL_KEY, avatarUrl).also { jsonObject ->
                run {
                    for (entry in additionalKeyValues) {
                        jsonObject.put(entry.key, entry.value)
                    }
                }
            }

    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .optionalProperty(PROFILE_DISPLAY_NAME_KEY, Schemas.stringSchema())
                .optionalProperty(PROFILE_AVATAR_URL_KEY, Schemas.stringSchema())
                .toJson()
        )

    override fun validate(): Boolean {
        return DTO.validate(asJson(), schema)
    }

    companion object {
        val PROFILE_DISPLAY_NAME_KEY = matrixFields.getString("profile.displayname")
        val PROFILE_AVATAR_URL_KEY = matrixFields.getString("profile.avatarUrl")

        fun fromJson(jsonObject: JsonObject): UserProfile {
            val copyJson = JsonObject.mapFrom(jsonObject.map)
            return UserProfile(
                jsonObject.getString(PROFILE_DISPLAY_NAME_KEY), jsonObject.getString(
                    PROFILE_AVATAR_URL_KEY
                ), copyJson.apply {
                    this.remove(PROFILE_DISPLAY_NAME_KEY)
                    this.remove(PROFILE_AVATAR_URL_KEY)
                }.map
            )
        }
    }

}
