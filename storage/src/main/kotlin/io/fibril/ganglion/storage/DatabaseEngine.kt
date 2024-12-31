package io.fibril.ganglion.storage

import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlConnectOptions


abstract class DatabaseEngine {

    abstract suspend fun connectOptions(): SqlConnectOptions

    abstract suspend fun poolOptions(): PoolOptions
}