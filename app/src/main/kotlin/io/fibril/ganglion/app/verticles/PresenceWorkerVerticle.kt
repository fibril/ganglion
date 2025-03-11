package io.fibril.ganglion.app.verticles

import com.google.inject.Guice
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.presence.Presence
import io.fibril.ganglion.clientServer.v1.presence.PresenceActions
import io.fibril.ganglion.clientServer.v1.presence.PresenceService
import io.fibril.ganglion.clientServer.v1.presence.dtos.PutPresenceDTO
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.ThreadingModel
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.future.await
import java.util.*

class PresenceWorkerVerticle : CoroutineVerticle() {
    private lateinit var presenceService: PresenceService

    override suspend fun start() {
        val injector = Guice.createInjector(ClientModule(vertx))
        presenceService = injector.getInstance(PresenceService::class.java)

        val eventBus = vertx.eventBus()

        eventBus.consumer(PresenceActions.USER_ONLINE) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                userOnline(message)
                    .onSuccess { isSaved ->
                        if (isSaved) {
                            vertx.setTimer(USER_IDLE_TIMEOUT) {
                                checkIfUserIsStillActive(message)
                            }
                        }
                    }
                    .onFailure { e ->
                        println("userOnline Failed from Verticle ${e.message}")
                    }
            }

        }

        eventBus.consumer(PresenceActions.USER_OFFLINE) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                userOffline(message)
                    .onSuccess { isSaved ->
                        println("userOffline?: $isSaved")
                    }
                    .onFailure { e ->
                        println("userOffline Failed from Verticle ${e.message}")
                    }
            }

        }
    }

    private suspend fun userOnline(message: Message<JsonObject>): Future<Boolean> {
        val body = message.body()
        val userId = body.getString("sub") ?: return Future.succeededFuture(false)
        return changePresence(userId, "online")
    }

    private suspend fun userOffline(message: Message<JsonObject>): Future<Boolean> {
        val body = message.body()
        val userId = body.getString("sub") ?: return Future.succeededFuture(false)
        return changePresence(userId, "offline")
    }

    private suspend fun changePresence(userId: String, presenceStatus: String): Future<Boolean> {
        try {
            var currentPresence: Presence? = null
            try {
                currentPresence =
                    presenceService.findOne(userId).toCompletionStage().await() ?: return Future.succeededFuture(false)
            } catch (e: Exception) {
                //
            }
            val currentPresenceContent = currentPresence?.asJson()?.getJsonObject("content") ?: JsonObject()

            presenceService.update(
                userId,
                PutPresenceDTO(
                    json = currentPresenceContent.put("presence", presenceStatus),
                    sender = null
                )
            ).toCompletionStage().await()
            return Future.succeededFuture(true)
        } catch (e: Exception) {
            return Future.failedFuture(e)
        }
    }

    private fun checkIfUserIsStillActive(message: Message<JsonObject>) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            try {
                val body = message.body()
                val userId = body.getString("sub") ?: return@usingCoroutineScopeWithIODispatcher
                val currentPresence =
                    presenceService.findOne(userId).toCompletionStage().await()
                        ?: return@usingCoroutineScopeWithIODispatcher
                val updatedAt =
                    currentPresence.asJson().getJsonObject("content")?.getLong("updated_at")
                        ?: return@usingCoroutineScopeWithIODispatcher
                if (Date().time - updatedAt >= USER_IDLE_TIMEOUT) {
                    userOffline(message)
                }
            } catch (e: Exception) {
                //
            }
        }
    }

    companion object {
        val USER_IDLE_TIMEOUT = 1000 * 60 * 5L // 5 minutes

        val deploymentOptions = DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER).setInstances(2)
            .setWorkerPoolName("presence-worker-pool")
            .setWorkerPoolSize(2)

    }
}