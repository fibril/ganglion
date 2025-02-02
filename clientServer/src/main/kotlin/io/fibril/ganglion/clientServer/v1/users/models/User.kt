package io.fibril.ganglion.clientServer.v1.users.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject

interface UserModel : Model {

}

data class User @Inject constructor(
    val id: String,
    val fullJsonObject: JsonObject? = null
) : UserModel {
    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    companion object {
        val PasswordRegex = Regex("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$")
        val UsernameRegex = Regex("""^[a-zA-Z0-9_\-=./]+$""")
    }

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())
}
