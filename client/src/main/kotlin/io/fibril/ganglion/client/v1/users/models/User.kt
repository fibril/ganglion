package io.fibril.ganglion.client.v1.users.models

import com.google.inject.Inject
import io.fibril.ganglion.client.Model
import io.vertx.core.json.JsonObject

interface UserModel : Model {

}

data class User @Inject constructor(
    val userId: MatrixUserId,
    val otherJsonObject: JsonObject? = null
) : UserModel {

    override fun asJson(): JsonObject =
        JsonObject()
            .put(ID_FIELD_KEY, userId.toString())
            .put(USER_ID_FIELD_KEY, userId.toString())
            .mergeIn(otherJsonObject ?: JsonObject())

    companion object {
        private const val ID_FIELD_KEY = "id"
        private const val USER_ID_FIELD_KEY = "user_id"

        fun fromJson(jsonObject: JsonObject): User {
            return User(
                MatrixUserId(jsonObject.getString(ID_FIELD_KEY) ?: jsonObject.getString(USER_ID_FIELD_KEY)),
                jsonObject.copy().apply {
                    this.remove(ID_FIELD_KEY)
                    // remove other unwanted values
                }
            )
        }


        val PasswordRegex = Regex("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$")
        val UsernameRegex = Regex("""^[a-zA-Z0-9_\-=./]+$""")
    }


}
