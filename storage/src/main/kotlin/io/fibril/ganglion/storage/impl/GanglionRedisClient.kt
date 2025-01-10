package io.fibril.ganglion.storage.impl

import StorageConstants
import com.google.inject.Inject
import io.fibril.ganglion.storage.Database
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.sqlclient.Pool

class GanglionRedisClient @Inject constructor(val vertx: Vertx) : Database<Redis> {
    override suspend fun client(): Redis {
        val config = StorageConstants.config(vertx).await()
        val redisConfig = config.getJsonObject("ganglion").getJsonObject("redis")
        return Redis.createClient(vertx, redisConfig.getString("endpoint"))
    }

    override suspend fun pool(): Pool {
        TODO("Not yet implemented")
    }
}