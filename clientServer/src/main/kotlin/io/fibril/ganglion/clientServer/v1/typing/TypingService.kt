package io.fibril.ganglion.clientServer.v1.typing

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.storage.impl.GanglionRedisClient
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import kotlinx.coroutines.future.await


interface TypingService : Service<Typing> {
    suspend fun getTypingUsers(roomId: String): Future<Typing>
}

class TypingServiceImpl @Inject constructor(
    val vertx: Vertx,
    val ganglionRedisClient: GanglionRedisClient
) :
    TypingService {
    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<Typing> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<Typing>> {
        TODO()
    }

    override suspend fun findOne(id: String): Future<Typing?> {
        TODO("Not yet implemented")
    }


    /**
     * Get Users typing in the room
     */
    override suspend fun getTypingUsers(roomId: String): Future<Typing> {
        val key = makeKey(roomId)
        val redisClient = ganglionRedisClient.client()
        val redisAPI = RedisAPI.api(redisClient)
        val userIdsResponse = redisAPI.hgetall(key).toCompletionStage().await()
        return Future.succeededFuture(Typing(RoomId(roomId), JsonArray(userIdsResponse?.keys?.toList())))
    }

    override suspend fun update(roomId: String, dto: DTO): Future<Typing> {
        var redisClient: Redis? = null
        try {
            val key = makeKey(roomId)
            val params = dto.params()
            val isTyping = params.getBoolean("typing", false)
            val userId = params.getString("userId")

            redisClient = ganglionRedisClient.client()
            val redisAPI = RedisAPI.api(redisClient)

            if (isTyping) {
                redisAPI.hset(listOf(key, userId, "")).toCompletionStage().await()
            } else {
                redisAPI.hdel(listOf(key, userId)).toCompletionStage().await()
            }

            vertx.eventBus().send(TypingActions.TYPING_UPDATED, params)
            return Future.succeededFuture(Typing(RoomId(roomId), JsonArray.of(userId)))
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

    override suspend fun remove(id: String): Future<Typing> {
        TODO("Not yet implemented")
    }

    private fun makeKey(roomId: String): String =
        if (roomId.substringAfterLast(':') == KEY_SUFFIX) roomId else "$roomId:$KEY_SUFFIX"


    companion object {
        const val IDENTIFIER = "v1.typing.TypingService"
        const val KEY_SUFFIX = "typing"
    }
}