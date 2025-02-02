package io.fibril.ganglion.clientServer

import io.vertx.core.json.JsonObject

interface Model {

    fun asJson(): JsonObject

}