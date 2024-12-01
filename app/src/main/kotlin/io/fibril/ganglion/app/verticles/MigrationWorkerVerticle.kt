package io.fibril.ganglion.app.verticles

import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import io.vertx.core.ThreadingModel
import kotlinx.coroutines.runBlocking

class MigrationWorkerVerticle : AbstractVerticle() {
    override fun start(promise: Promise<Void>) {
        val pgDatabase = PGDatabase()
        runBlocking {
            pgDatabase.migrate { result ->
                run {
                    if (result.isSuccess) {
                        promise.complete()
                    } else {
                        promise.fail(result.exceptionOrNull())
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