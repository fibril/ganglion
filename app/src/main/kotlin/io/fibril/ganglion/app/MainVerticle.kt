package io.fibril.ganglion.app

import com.google.inject.Guice
import io.fibril.ganglion.app.verticles.MigrationWorkerVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import v1.RoutesV1
import v1.users.UserModule
import v1.users.services.UserProfileService
import v1.users.services.UserService
import java.lang.reflect.Field


class MainVerticle : CoroutineVerticle() {
    override suspend fun start() {
        deployMigrationWorkerVerticle(vertx).onComplete {
            if (it.succeeded()) {
                val injector = Guice.createInjector(UserModule(vertx))
                val userService = injector.getInstance(UserService::class.java)
                val userProfileService = injector.getInstance(UserProfileService::class.java)

                val servicesMap = mapOf(
                    userService.identifier to userService,
                    userProfileService.identifier to userProfileService
                )

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

                for (r in router.routes) {
                    // Path is public, but methods are not. We change that
                    val f: Field = r.javaClass.getDeclaredField("state")
                    f.setAccessible(true)
                    println(f.get(r).toString() + r.path)
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