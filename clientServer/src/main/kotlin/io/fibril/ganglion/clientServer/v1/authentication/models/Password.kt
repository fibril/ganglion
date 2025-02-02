package io.fibril.ganglion.clientServer.v1.authentication.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject


interface PasswordModel : Model

data class Password @Inject constructor(
    val id: String, val fullJsonObject: JsonObject? = null
) : PasswordModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())


    companion object {

    }

}
