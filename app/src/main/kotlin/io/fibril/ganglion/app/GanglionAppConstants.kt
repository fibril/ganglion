package io.fibril.ganglion.app

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred

object GanglionAppConstants {
    val config: Deferred<JsonObject>
        get() {
            val hierarchicalConfig = ConfigStoreOptions().setFormat("properties").setType("file")
                .setConfig(JsonObject().put("path", "application.properties").put("hierarchical", true))
            val options = ConfigRetrieverOptions().addStore(hierarchicalConfig)
            val configRetriever = ConfigRetriever.create(Vertx.vertx(), options)
            return configRetriever.config.toCompletionStage().asDeferred()
        }
}


