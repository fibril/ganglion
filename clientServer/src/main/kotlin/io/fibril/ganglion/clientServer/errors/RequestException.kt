package io.fibril.ganglion.clientServer.errors

import io.vertx.core.VertxException
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException

class RequestException(val statusCode: Int, override val message: String, val json: JsonObject) :
    VertxException(message) {
    companion object {
        fun fromPgException(e: PgException): RequestException = RequestException(
            e.errorCode,
            e.message ?: "Unknown Error",
            StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
        )
    }
}

