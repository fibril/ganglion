package io.fibril.ganglion.app

import io.fibril.ganglion.app.verticles.MigrationWorkerVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle

class MainVerticle : CoroutineVerticle() {
    override suspend fun start() {
        deployMigrationWorkerVerticle(vertx).onComplete {
            if (it.succeeded()) {
                println("Migration Succeeded")
            } else {
                println("Migration failed")
                println(it.cause().message)
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