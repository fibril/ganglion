package io.fibril.ganglion.app.verticles

import io.fibril.ganglion.storage.impl.PGDatabase
import io.fibril.ganglion.storage.migration.Migrator
import io.vertx.core.DeploymentOptions
import io.vertx.core.ThreadingModel
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.runBlocking

class MigrationWorkerVerticle : CoroutineVerticle() {
    override suspend fun start() {
        val pgDatabaseClient = PGDatabase(vertx).client()
        runBlocking {
            pgDatabaseClient.query(Migrator.buildMigrationSQL()).execute().onComplete { ar ->
                if (ar.failed()) {
                    throw ar.cause()
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