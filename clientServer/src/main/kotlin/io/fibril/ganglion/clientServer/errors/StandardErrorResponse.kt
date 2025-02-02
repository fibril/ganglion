package io.fibril.ganglion.clientServer.errors

import io.vertx.core.json.JsonObject
import io.fibril.ganglion.clientServer.utils.SupportedLanguages
import java.util.*

data class StandardErrorResponse(
    val errCode: ErrorCodes,
    val error: String? = null,
    val additionalKeyValues: JsonObject = JsonObject()
) {
    internal constructor(errCode: ErrorCodes, forLanguages: SupportedLanguages, additionalKeyValues: JsonObject) : this(
        errCode,
        ResourceBundle.getBundle("${forLanguages.name.lowercase()}.errors").getString(errCode.name),
        additionalKeyValues
    )

    internal constructor(errCode: ErrorCodes, additionalKeyValues: JsonObject) : this(
        errCode,
        SupportedLanguages.EN,
        additionalKeyValues
    )

    internal constructor(errCode: ErrorCodes) : this(errCode, SupportedLanguages.EN, JsonObject())


    fun asJson(): JsonObject =
        JsonObject()
            .put("errCode", errCode)
            .put("errcode", errCode)
            .put("error", error)
            .mergeIn(additionalKeyValues)

    override fun toString() = asJson().toString()
}
