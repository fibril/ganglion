package io.fibril.ganglion.app

import com.google.inject.Guice
import io.fibril.ganglion.app.verticles.*
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.v1.RoutesV1
import io.fibril.ganglion.clientServer.v1.authentication.AuthService
import io.fibril.ganglion.clientServer.v1.media.MediaService
import io.fibril.ganglion.clientServer.v1.presence.PresenceService
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.rooms.RoomAliasService
import io.fibril.ganglion.clientServer.v1.rooms.RoomService
import io.fibril.ganglion.clientServer.v1.typing.TypingService
import io.fibril.ganglion.clientServer.v1.users.UserProfileService
import io.fibril.ganglion.clientServer.v1.users.UserService
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle


class MainVerticle : CoroutineVerticle() {
    override suspend fun start() {
        deployMigrationWorkerVerticle(vertx).onComplete {
            if (it.succeeded()) {
                deployWorkerVerticles(vertx).onComplete {
                    println("Worker Verticles deployed")
                }.onFailure {
                    vertx.close()
                }

                val injector = Guice.createInjector(ClientModule(vertx))

                val services = listOf(
                    injector.getInstance(AuthService::class.java),
                    injector.getInstance(MediaService::class.java),
                    injector.getInstance(PresenceService::class.java),
                    injector.getInstance(RoomAliasService::class.java),
                    injector.getInstance(RoomEventService::class.java),
                    injector.getInstance(RoomService::class.java),
                    injector.getInstance(TypingService::class.java),
                    injector.getInstance(UserProfileService::class.java),
                    injector.getInstance(UserService::class.java)
                )

                val servicesMap: Map<String, Service<*>> = mutableMapOf<String, Service<*>>().apply {
                    for (service in services) {
                        this[service.identifier] = service
                    }
                }

                val router = RoutesV1(vertx, servicesMap).router

                vertx
                    .createHttpServer()
                    .requestHandler(router)
                    .listen(8888).onComplete { http ->
                        if (http.succeeded()) {
                            println("HTTP server started on port 8888")
                        } else {
                        }
                    }
            } else {
                vertx.close()
            }
        }
    }

    private fun deployMigrationWorkerVerticle(vertx: Vertx): Future<String> {
        return vertx.deployVerticle(MigrationWorkerVerticle::class.java, MigrationWorkerVerticle.deploymentOptions)
    }

    private fun deployWorkerVerticles(vertx: Vertx): Future<String> {
        return vertx.deployVerticle(RoomEventsWorkerVerticle::class.java, RoomEventsWorkerVerticle.deploymentOptions)
            .andThen {
                vertx.deployVerticle(AuthWorkerVerticle::class.java, AuthWorkerVerticle.deploymentOptions)
            }
            .andThen {
                vertx.deployVerticle(UserWorkerVerticle::class.java, UserWorkerVerticle.deploymentOptions)
            }
            .andThen {
                vertx.deployVerticle(PresenceWorkerVerticle::class.java, PresenceWorkerVerticle.deploymentOptions)
            }
            .andThen {
                vertx.deployVerticle(MediaWorkerVerticle::class.java, MediaWorkerVerticle.deploymentOptions)
            }
            .andThen {
                vertx.deployVerticle(TypingWorkerVerticle::class.java, TypingWorkerVerticle.deploymentOptions)
            }

    }
}