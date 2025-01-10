package extensions

import io.vertx.core.json.JsonObject

fun JsonObject.only(keys: Set<String>): JsonObject {
    return JsonObject().also {
        for (field in keys) {
            it.put(field, this.getValue(field))
        }
    }
}

fun JsonObject.exclude(keys: Set<String>): JsonObject {
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