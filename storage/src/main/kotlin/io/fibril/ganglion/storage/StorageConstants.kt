import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred

object StorageConstants {
    val configRetrieverOptions: ConfigRetrieverOptions
        get() {
            val hierarchicalConfig = ConfigStoreOptions().setFormat("properties").setType("file")
                .setConfig(JsonObject().put("path", "database.properties").put("hierarchical", true))
            return ConfigRetrieverOptions().addStore(hierarchicalConfig)
        }

    fun config(vertx: Vertx): Deferred<JsonObject> {
        val configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions)
        return configRetriever.config.toCompletionStage().asDeferred()
    }
}