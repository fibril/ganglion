package io.fibril.ganglion.storage.impl

import StorageConstants
import io.fibril.ganglion.storage.Database
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.SqlClient
import org.flywaydb.core.Flyway

class PGDatabase : Database(PGEngine()) {
    override suspend fun client(vertx: Vertx): SqlClient {
        val connectOptions = dbEngine.connectOptions()
        val poolOptions = dbEngine.poolOptions()
        return PgBuilder.client()
            .with(poolOptions)
            .connectingTo(connectOptions)
            .using(vertx)
            .build()
    }

    override suspend fun migrate(handler: Handler<Result<Boolean>>) {
        try {
            val flywayConfiguration = StorageConstants.flywayConfig
            val flyway = Flyway(flywayConfiguration)
            flyway.migrate()
            handler.handle(Result.success(true))
        } catch (e: Exception) {
            handler.handle(Result.failure(e))
        }
    }
}