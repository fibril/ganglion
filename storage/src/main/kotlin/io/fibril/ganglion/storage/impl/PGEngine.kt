package io.fibril.ganglion.storage.impl

import StorageConstants
import io.fibril.ganglion.storage.DatabaseEngine
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions

class PGEngine(private val vertx: Vertx) : DatabaseEngine() {

    override suspend fun connectOptions(): PgConnectOptions {
        val config = StorageConstants.config(vertx).await()
        val dbConfig = config.getJsonObject("ganglion").getJsonObject("db")
        val credentialsObj = dbConfig.getJsonObject("credentials")
        return PgConnectOptions()
            .setPort(dbConfig.getInteger("port") ?: PgConnectOptions.DEFAULT_PORT)
            .setHost(dbConfig.getString("host"))
            .setDatabase(dbConfig.getString("name"))
            .setUser(credentialsObj.getString("username"))
            .setPassword(credentialsObj.getString(("password")))
    }

    override suspend fun poolOptions(): PoolOptions {
        val config = StorageConstants.config(vertx).await()
        val dbConfig = config.getJsonObject("ganglion").getJsonObject("db")
        val poolObject = dbConfig.getJsonObject("pool")
        val poolOptions = PoolOptions()
        return poolOptions
            .setName(poolObject.getString("name"))
            .setMaxSize(poolObject?.getInteger("maxSize") ?: PoolOptions.DEFAULT_MAX_SIZE)
            .setMaxLifetime(poolObject?.getInteger("lifeTime") ?: PoolOptions.DEFAULT_MAXIMUM_LIFETIME)
    }
}