package io.fibril.ganglion.app.verticles

import com.google.inject.Guice
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.extensions.exclude
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.authentication.AuthService
import io.fibril.ganglion.clientServer.v1.authentication.models.AuthDatabaseActions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.ThreadingModel
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

class AuthWorkerVerticle : CoroutineVerticle() {
    private lateinit var authService: AuthService

    override suspend fun start() {
        val injector = Guice.createInjector(ClientModule(vertx))
        authService = injector.getInstance(AuthService::class.java)

        val eventBus = vertx.eventBus()

        eventBus.consumer(AuthDatabaseActions.TOKEN_CREATED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                tokenCreated(message)
                    .onSuccess { isSaved ->
                        println(if (isSaved) "Token Saved" else "Unable to save token")
                    }
                    .onFailure { e ->
                        println("tokenCreated Failed from Verticle ${e.message}")
                    }
            }

        }
    }

    private suspend fun tokenCreated(message: Message<JsonObject>): Future<Boolean> {
        try {
            val tokenDataObject = message.body()
            val token = tokenDataObject.getString("token")
            return authService.saveGeneratedToken(token, tokenDataObject.exclude("token"))
        } catch (e: Exception) {
            return Future.failedFuture(e)
        }
    }

    companion object {
        val deploymentOptions = DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER).setInstances(2)
            .setWorkerPoolName("auth-worker-pool")
            .setWorkerPoolSize(2)

    }
}