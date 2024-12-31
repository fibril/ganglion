package io.fibril.ganglion.storage.impl

import io.fibril.ganglion.storage.Database
import io.vertx.core.Vertx
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.SqlClient

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
}