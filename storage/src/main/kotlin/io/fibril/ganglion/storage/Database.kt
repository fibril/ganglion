package io.fibril.ganglion.storage

import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlClient


interface Database {
    suspend fun client(): SqlClient

    suspend fun pool(): Pool
}