package io.fibril.ganglion.app.verticles

import com.google.inject.Guice
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.storage.impl.GanglionOpenSearch
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.DeploymentOptions
import io.vertx.core.ThreadingModel
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.ExistsRequest
import java.io.IOException


enum class DBAction {
    CREATED,
    UPDATED,
    DELETED
}

class OpenSearchWorkerVerticle : CoroutineVerticle() {
    private lateinit var database: PGDatabase
    private lateinit var openSearchClient: OpenSearchClient

    val tables = arrayListOf("rooms", "room_events")

    override suspend fun start() {
        openSearchClient = GanglionOpenSearch().client()

        val injector = Guice.createInjector(ClientModule(vertx))
        database = injector.getInstance(PGDatabase::class.java)

        vertx.setTimer(40000L) {
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                subscribe()
            }
        }
    }

    private suspend fun subscribe() {
        val subscriber = database.subscriber()

        for (table in tables) {
            try {
                val exists = indexExists(table)
                println("$table index ${if (exists) "exists" else "does not exist"}")
                if (!exists) {
                    val createIndexRequest = CreateIndexRequest.Builder().index(table).build()
                    val indexCreatedResponse = openSearchClient.indices().create(createIndexRequest)
                    println("indexCreatedResponse ${indexCreatedResponse}")
                }
            } catch (e: IOException) {
                println("Error creating index: ${e}")
            } catch (e: OpenSearchException) {
                println("Error creating index: ${e}")
            }
        }

        subscriber
            .connect()
            .onComplete { ar ->
                if (ar.succeeded()) {
                    val actions = arrayListOf(DBAction.CREATED, DBAction.UPDATED, DBAction.DELETED)
                    for (tableName in tables) {
                        for (action in actions) {
                            val channelName = "${tableName}:${action.name.lowercase()}"
                            println("Subscribing to $channelName")
                            subscriber.channel(channelName)
                                .handler { payload ->
                                    dbResourceNotificationHandler(
                                        payload,
                                        tableName,
                                        action
                                    )
                                }
                        }
                    }
                } else {
                    println("Database Subsription failed ${ar.cause()}")
                }
            }
    }

    private fun dbResourceNotificationHandler(payload: String, openSearchIndex: String, dbAction: DBAction) {
        println("payload $payload")
        if (dbAction == DBAction.DELETED) {

        } else {
            val json = JsonObject(payload)
            val indexRequest: IndexRequest<JsonObject> =
                IndexRequest.Builder<JsonObject>().index(openSearchIndex).id(json.getString("id")).document(json)
                    .build()
            openSearchClient.index(indexRequest)
        }

    }

    private fun indexExists(index: String): Boolean {
        return openSearchClient.indices().exists(ExistsRequest.of { it.index(index) }).value()
    }

    companion object {
        val deploymentOptions = DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER).setInstances(1)
            .setWorkerPoolName("opensearch-worker-pool")
            .setWorkerPoolSize(2)

    }
}