package io.fibril.ganglion.app

import com.google.inject.Guice
import io.fibril.ganglion.app.verticles.MigrationWorkerVerticle
import io.fibril.ganglion.app.verticles.RoomEventsWorkerVerticle
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.v1.RoutesV1
import io.fibril.ganglion.clientServer.v1.authentication.AuthService
import io.fibril.ganglion.clientServer.v1.media.MediaService
import io.fibril.ganglion.clientServer.v1.rooms.RoomAliasService
import io.fibril.ganglion.clientServer.v1.rooms.RoomService
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
                }

                val injector = Guice.createInjector(ClientModule(vertx))

                val services = listOf(
                    injector.getInstance(UserService::class.java),
                    injector.getInstance(UserProfileService::class.java),
                    injector.getInstance(MediaService::class.java),
                    injector.getInstance(AuthService::class.java),
                    injector.getInstance(RoomService::class.java),
                    injector.getInstance(RoomAliasService::class.java)
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
                // block
            }
        }

//        val pgDatabase = PGDatabase()
//        val client = pgDatabase.client(vertx)
//        client.query("CREATE TABLE IF NOT EXISTS gangs ( name varchar );").execute().onComplete {
//            if (it.succeeded()) {
//                println("it succeeded")
//            } else {
//                println("it failed")
//            }
//        }
//        vertx.createHttpServer()
//            .listen(8888)
//            .onComplete { http ->
//                if (http.succeeded()) {
//                    println("HTTP server started on port 8888")
//                } else {
//                    println("HTTP server failed to start")
//                }
//
//            }
    }

    private fun deployMigrationWorkerVerticle(vertx: Vertx): Future<String> {
        return vertx.deployVerticle(MigrationWorkerVerticle::class.java, MigrationWorkerVerticle.deploymentOptions)
    }

    private fun deployWorkerVerticles(vertx: Vertx): Future<String> {
        return vertx.deployVerticle(RoomEventsWorkerVerticle::class.java, RoomEventsWorkerVerticle.deploymentOptions)
    }
}