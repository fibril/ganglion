package io.fibril.ganglion.app

import com.google.inject.Guice
import io.fibril.ganglion.app.verticles.MigrationWorkerVerticle
import io.fibril.ganglion.client.ClientModule
import io.fibril.ganglion.client.Service
import io.fibril.ganglion.client.v1.RoutesV1
import io.fibril.ganglion.client.v1.authentication.AuthService
import io.fibril.ganglion.client.v1.media.MediaService
import io.fibril.ganglion.client.v1.users.UserProfileService
import io.fibril.ganglion.client.v1.users.UserService
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle


class MainVerticle : CoroutineVerticle() {
    override suspend fun start() {
        deployMigrationWorkerVerticle(vertx).onComplete {
            if (it.succeeded()) {
                val injector = Guice.createInjector(ClientModule(vertx))

                val services = listOf(
                    injector.getInstance(UserService::class.java),
                    injector.getInstance(UserProfileService::class.java),
                    injector.getInstance(MediaService::class.java),
                    injector.getInstance(AuthService::class.java)
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
}