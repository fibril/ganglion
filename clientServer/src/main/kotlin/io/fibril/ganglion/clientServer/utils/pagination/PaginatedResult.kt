package io.fibril.ganglion.clientServer.utils.pagination

import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

interface Paginated {
    fun asJson(): JsonObject
}

data class PaginatedResult<T : Model>(
    val chunk: List<T>,
    val next_batch: String?,
    val prev_batch: String?,
    val additionalPayload: JsonObject?
) : Paginated {
    override fun asJson() = JsonObject().apply {
        put("chunk", JsonArray(chunk.map { it.asJson() }))
        if (next_batch != null) put("next_batch", next_batch)
        if (prev_batch != null) put("prev_batch", prev_batch)
        if (additionalPayload != null) mergeIn(additionalPayload)
    }
}