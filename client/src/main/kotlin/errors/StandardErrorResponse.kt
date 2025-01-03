package errors

import io.vertx.core.json.JsonObject

data class StandardErrorResponse(
    val errCode: String,
    val error: String,
    val additionalKeyValues: Map<String, String> = mapOf()
) {
    fun asJSON(): JsonObject =
        JsonObject()
            .put("errCode", errCode).put("errcode", errCode).put("error", error).also { jsonObject ->
                run {
                    for (entry in additionalKeyValues) {
                        jsonObject.put(entry.key, entry.value)
                    }
                }
            }
}
