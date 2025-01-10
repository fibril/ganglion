package io.fibril.ganglion.client.errors

import io.vertx.core.VertxException
import io.vertx.core.json.JsonObject

class RequestException(val statusCode: Int, override val message: String, val json: JsonObject) :
    VertxException(message)