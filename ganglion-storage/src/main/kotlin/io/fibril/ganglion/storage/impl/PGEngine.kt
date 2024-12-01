package io.fibril.ganglion.storage.impl

import StorageConstants
import io.fibril.ganglion.storage.DatabaseEngine
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions

class PGEngine : DatabaseEngine() {

    override suspend fun connectOptions(): PgConnectOptions {
        val config = StorageConstants.config.await()
        val credentialsObj = config.getJsonObject("credentials")
        return PgConnectOptions()
            .setPort(config.getInteger("port") ?: PgConnectOptions.DEFAULT_PORT)
            .setHost(config.getString("host"))
            .setDatabase(config.getString("name"))
            .setUser(credentialsObj.getString("username"))
            .setPassword(credentialsObj.getString(("password")))
    }

    override suspend fun poolOptions(): PoolOptions {
        val config = StorageConstants.config.await()
        val poolObject = config.getJsonObject("pool")
        val poolSizeObject = poolObject.getJsonObject("size") ?: null
        val poolTimeObject = poolObject.getJsonObject("time") ?: null
        val poolOptions = PoolOptions()
        return poolOptions.setMaxSize(poolSizeObject?.getInteger("max") ?: PoolOptions.DEFAULT_MAX_SIZE)
            .setMaxLifetime(poolTimeObject?.getInteger("maxLifetime") ?: PoolOptions.DEFAULT_MAXIMUM_LIFETIME)
    }
}