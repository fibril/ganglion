package io.fibril.ganglion.storage

import io.vertx.sqlclient.SqlClient


interface Database {
    suspend fun client(): SqlClient

}