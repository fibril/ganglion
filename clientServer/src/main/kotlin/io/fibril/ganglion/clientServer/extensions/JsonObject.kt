package io.fibril.ganglion.clientServer.extensions

import io.vertx.core.json.JsonObject

fun JsonObject.only(vararg keys: String): JsonObject {
    return JsonObject().also {
        for (field in keys) {
            it.put(field, this.getValue(field))
        }
    }
}

fun JsonObject.exclude(vararg keys: String): JsonObject {
    val currentJsonObject = this.copy()
    if (keys.isNotEmpty()) {
        return currentJsonObject.also {
            for (field in keys) {
                it.remove(field)
            }
        }
    }
    return currentJsonObject
}