package io.fibril.ganglion.storage.impl

import com.google.inject.Inject
import io.fibril.ganglion.storage.Database
import io.vertx.core.Vertx
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.SqlClient

class PGDatabase @Inject constructor(val vertx: Vertx) : Database {
    private val dbEngine = PGEngine(vertx)

    override suspend fun client(): SqlClient {
        val connectOptions = dbEngine.connectOptions()
        val poolOptions = dbEngine.poolOptions()
        return PgBuilder.client()
            .with(poolOptions)
            .connectingTo(connectOptions)
            .using(vertx)
            .build()
    }
}