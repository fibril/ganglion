package io.fibril.ganglion.client.v1.authentication.models

import com.google.inject.Inject
import io.fibril.ganglion.client.Model
import io.fibril.ganglion.client.v1.users.models.MatrixUserId
import io.fibril.ganglion.client.v1.users.models.User
import io.vertx.core.json.JsonObject


interface PasswordModel : Model

data class Password @Inject constructor(val id: String, val hash: String, val user: User) : PasswordModel {
    override fun asJson(): JsonObject {
        return JsonObject()
            .put("id", id)
            .put("hash", hash)
            .put("user_id", user.userId.toString())
    }

    companion object {
        fun fromJson(jsonObject: JsonObject): Password {
            return Password(
                jsonObject.getString("id"),
                jsonObject.getString("hash"),
                User(MatrixUserId(jsonObject.getString("user_id")))
            )
        }
    }

}
