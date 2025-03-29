package io.fibril.ganglion.app.verticles

import com.google.inject.Guice
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.typing.TypingActions
import io.fibril.ganglion.clientServer.v1.typing.TypingService
import io.fibril.ganglion.clientServer.v1.typing.dtos.PutTypingDTO
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.ThreadingModel
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

class TypingWorkerVerticle : CoroutineVerticle() {
    private lateinit var typingService: TypingService

    override suspend fun start() {
        val injector = Guice.createInjector(ClientModule(vertx))
        typingService = injector.getInstance(TypingService::class.java)

        val eventBus = vertx.eventBus()

        eventBus.consumer(TypingActions.TYPING_UPDATED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                userTypingUpdated(message)
                    .onSuccess { handled ->
                        if (handled) println("typing handled") else println("typing not handled")
                    }
                    .onFailure { e ->
                        println("userTypingUpdated Failed from Verticle ${e.message}")
                    }
            }

        }
    }

    private suspend fun userTypingUpdated(message: Message<JsonObject>): Future<Boolean> {
        val body = message.body()
        val roomId = body.getString("roomId") ?: return Future.succeededFuture(false)
        val isTyping = body.getBoolean("typing", false)
        val timeout = body.getInteger("timeout", DEFAULT_TYPING_DURATION)
        if (isTyping) {
            val putCancelTypingDTO = PutTypingDTO(body.put("typing", false))
            vertx.setTimer(timeout.toLong()) {
                CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                    typingService.update(roomId, putCancelTypingDTO)
                }
            }
        }
        return Future.succeededFuture(true)
    }

    companion object {
        const val DEFAULT_TYPING_DURATION = 1000 * 30 // 30 seconds

        val deploymentOptions = DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER).setInstances(2)
            .setWorkerPoolName("typing-worker-pool")
            .setWorkerPoolSize(2)

    }
}