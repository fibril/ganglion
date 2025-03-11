package io.fibril.ganglion.storage.impl

import StorageConstants
import com.google.inject.Inject
import io.fibril.ganglion.storage.Database
import io.vertx.core.Vertx
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisOptions
import io.vertx.sqlclient.Pool

class GanglionRedisClient @Inject constructor(val vertx: Vertx) : Database<Redis> {
    override suspend fun client(): Redis {
        return Redis.createClient(vertx, redisOptions())
    }

    override suspend fun pool(): Pool {
        TODO("Not yet implemented")
    }

    private suspend fun redisOptions(): RedisOptions {
        val config = StorageConstants.config(vertx).await()
        val redisConfig = config.getJsonObject("ganglion").getJsonObject("redis")
        val connectionString = redisConfig.getString("endpoint")
        val maxPoolSize = redisConfig.getInteger("maxPoolSize")
        val maxPoolWaitingHandlers = redisConfig.getInteger("maxPoolWaitingHandlers")
        return RedisOptions().setConnectionString(connectionString)
            .setMaxPoolSize(maxPoolSize)
            .setMaxWaitingHandlers(maxPoolWaitingHandlers)
    }
}