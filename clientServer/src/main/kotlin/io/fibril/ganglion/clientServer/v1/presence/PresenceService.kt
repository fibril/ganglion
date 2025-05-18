package io.fibril.ganglion.clientServer.v1.presence

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.exclude
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.fibril.ganglion.storage.impl.GanglionRedisClient
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import kotlinx.coroutines.future.await
import java.util.*


interface PresenceService : Service<Presence> {
}

class PresenceServiceImpl @Inject constructor(
    val ganglionRedisClient: GanglionRedisClient
) :
    PresenceService {
    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<Presence> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<Presence>> {
        TODO()
    }

    override suspend fun findOne(userId: String): Future<Presence?> {
        val key = makeKey(userId)
        val redisClient = ganglionRedisClient.client()
        val redisAPI = RedisAPI.api(redisClient)
        try {
            val response = redisAPI.get(key).toCompletionStage().await()
            if (response != null) {
                return Future.succeededFuture(Presence(JsonObject(response.toString())))
            }
            return Future.succeededFuture(null)
        } catch (e: Exception) {
            println("Failed to set or retrieve key: ${e.message}")
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        } finally {
            redisClient.close()
        }
    }


    override suspend fun update(userId: String, dto: DTO): Future<Presence> {
        var redisClient: Redis? = null
        try {
            val key = makeKey(userId)
            val contentJson = dto.params().exclude("userId").put("updated_at", Date().time)
            val json = Presence(MatrixUserId(userId), contentJson).asJson()
            redisClient = ganglionRedisClient.client()
            val redisAPI = RedisAPI.api(redisClient)
            redisAPI.set(listOf(key, json.toString())).toCompletionStage().await()
            return Future.succeededFuture(Presence(MatrixUserId(userId), json))
        } catch (e: Exception) {
            println("Failed to set or retrieve key: ${e.message}")
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        } finally {
            redisClient?.close()
        }
    }

    override suspend fun remove(id: String): Future<Presence> {
        TODO("Not yet implemented")
    }


    companion object {
        const val IDENTIFIER = "v1.presence.PresenceService"
        const val KEY_SUFFIX = "presence"

        fun makeKey(userId: String): String =
            if (userId.substringAfterLast(':') == KEY_SUFFIX) userId else "$userId:$KEY_SUFFIX"
    }
}