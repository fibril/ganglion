package io.fibril.ganglion.app.verticles

import io.fibril.ganglion.storage.impl.PGDatabase
import io.fibril.ganglion.storage.migration.Migrator
import io.vertx.core.DeploymentOptions
import io.vertx.core.ThreadingModel
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import kotlinx.coroutines.runBlocking

class MigrationWorkerVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val pgDatabaseClient = PGDatabase().client(vertx)
        runBlocking {
            pgDatabaseClient.query(Migrator.buildMigrationSQL()).execute().onComplete { ar ->

                run {
                    if (ar.succeeded()) {
                        val result: RowSet<Row> = ar.result()
                        println("Got " + result.size() + " rows ")
                    } else {
                        println("Failure: " + ar.cause().message)
                    }

                }

            }
        }
    }

    companion object {
        val deploymentOptions =
            DeploymentOptions()
                .setThreadingModel(ThreadingModel.WORKER).setInstances(1)
                .setWorkerPoolName("db-migrations-worker-pool")
                .setWorkerPoolSize(1)
    }
}