package io.fibril.ganglion.client

import io.vertx.core.json.JsonObject

interface Model {

    fun asJson(): JsonObject


}