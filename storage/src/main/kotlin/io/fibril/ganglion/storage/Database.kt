package io.fibril.ganglion.storage

import io.vertx.core.Vertx
import io.vertx.sqlclient.SqlClient


abstract class Database(engine: DatabaseEngine) {
    val dbEngine = engine
    abstract suspend fun client(vertx: Vertx): SqlClient

}