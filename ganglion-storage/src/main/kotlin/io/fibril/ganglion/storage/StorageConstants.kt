import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.api.configuration.FluentConfiguration

object StorageConstants {
    val config: Deferred<JsonObject>
        get() {
            val hierarchicalConfig = ConfigStoreOptions().setFormat("properties").setType("file")
                .setConfig(JsonObject().put("path", "database.properties").put("hierarchical", true))
            val options = ConfigRetrieverOptions().addStore(hierarchicalConfig)
            val configRetriever = ConfigRetriever.create(Vertx.vertx(), options)
            return configRetriever.config.toCompletionStage().asDeferred()
        }

    val flywayConfig: FluentConfiguration
        get() {
            var configuration: FluentConfiguration
            runBlocking {
                val dbConfig = config.await().getJsonObject("ganglion").getJsonObject("db")
                val credentialsObject = dbConfig.getJsonObject("credentials")
                val url =
                    "jdbc:${dbConfig.getString("engine")}://${dbConfig.getString("host")}:${dbConfig.getString("port")}/${
                        dbConfig.getString("name")
                    }"
                configuration = FluentConfiguration().dataSource(
                    url,
                    credentialsObject.getString("username"),
                    credentialsObject.getString("password")
                )
            }
            return configuration
        }


}