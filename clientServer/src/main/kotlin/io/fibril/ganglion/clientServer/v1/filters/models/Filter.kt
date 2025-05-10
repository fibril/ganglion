package io.fibril.ganglion.clientServer.v1.filters.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.fibril.ganglion.clientServer.extensions.exclude
import io.vertx.core.json.JsonObject


interface FilterModel : Model

data class Filter @Inject constructor(
    val id: String,
    val fullJsonObject: JsonObject? = null
) : FilterModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson(): JsonObject = JsonObject().put("id", id)
        .mergeIn(
            fullJsonObject?.exclude("content") ?: JsonObject()
        )
        .mergeIn(
            fullJsonObject?.getJsonObject("content") ?: JsonObject()
        )
}
