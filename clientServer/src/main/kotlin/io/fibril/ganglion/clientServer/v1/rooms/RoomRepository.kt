package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.rooms.dtos.*
import io.fibril.ganglion.clientServer.v1.rooms.models.Room
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAlias
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.await


interface RoomRepository : Repository<Room> {
    suspend fun createRoomAlias(createRoomAliasDTO: CreateRoomAliasDTO): RoomAlias?
    suspend fun getRoomAlias(getRoomAliasDTO: GetRoomAliasDTO): RoomAlias?
    suspend fun getAliases(getAliasesDTO: GetAliasesDTO): List<RoomAlias>?
    suspend fun putRoomAlias(putRoomAliasDTO: PutRoomAliasDTO): RoomAlias?
    suspend fun deleteRoomAlias(deleteRoomAliasDTO: DeleteRoomAliasDTO): RoomAlias?
}


class RoomRepositoryImpl @Inject constructor(private val database: PGDatabase) : RoomRepository {
    override suspend fun save(dto: DTO): Room {
        val params = dto.params()
        val id = params.getString("id")
        val creatorId = (dto as CreateRoomDTO).sender?.principal()?.getString("sub")
        val isDirect = params.getString("is_direct") ?: false
//        val roomAliasName = createRoomDTO.json.getString("room_alias_name")
        val type = params.getString("type")
        val version = params.getString("room_version")
        val visibility = params.getString("visibility")

        val pool = database.pool()

        val result: Promise<JsonObject> = Promise.promise()
        var error: DatabaseException? = null

        pool.connection
            .onSuccess { conn ->
                conn.begin()
                    .compose { tx ->
                        conn.preparedQuery(CREATE_ROOM_QUERY).execute(
                            Tuple.of(
                                id,
                                creatorId,
                                isDirect,
                                type,
                                version,
                                visibility
                            )
                        ).compose { createRoomResponse ->
                            val txResponse = tx.commit()
                            val obj =
                                Room(
                                    id,
                                    createRoomResponse?.first()?.toJson()
                                ).asJson()
                            result.complete(obj)
                            txResponse
                        }
                    }.eventually { v -> conn.close() }
                    .onSuccess { }
                    .onFailure { err ->
                        run {
                            error = PgException(
                                err.message,
                                "SEVERE",
                                "500",
                                err.message
                            )
                            result.fail(error);
                        }
                    }
            }

        val resPayload = result.future().toCompletionStage().await()

        if (resPayload == null && error != null) {
            throw error!!
        }

        return Room(resPayload)
    }

    override suspend fun find(id: String): Room? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()
        var error: PgException? = null

        client.preparedQuery(GET_ROOM_QUERY).execute(Tuple.of(id)).onSuccess { res ->
            run {
                try {
                    result.complete(res.first().toJson())
                } catch (e: NoSuchElementException) {
                    result.complete(null)
                }
            }
        }
            .onFailure { err ->
                run {
                    error = PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                    result.fail(error);
                }
            }

        val resPayload = result.future().toCompletionStage().await()

        if (resPayload == null && error != null) {
            throw error!!
        }

        if (resPayload == null) {
            return null
        }

        return Room(resPayload)
    }


    override suspend fun findAll(): List<Room> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Room? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): Room {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()
        var error: PgException? = null

        client.preparedQuery(DELETE_ROOM_QUERY).execute(Tuple.of(id))
            .onSuccess { res ->
                run {
                    try {
                        result.complete(res.first().toJson())
                    } catch (e: NoSuchElementException) {
                        error = PgException(
                            e.message,
                            "SEVERE",
                            "500",
                            e.message
                        )
                        result.fail(e)
                    }
                }
            }
            .onFailure { err ->
                run {
                    error = PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                    result.fail(error);
                }
            }

        val resPayload = result.future().toCompletionStage().await()

        if (resPayload == null && error != null) {
            throw error!!
        }

        return Room(resPayload!!)
    }


    // RoomAlias

    override suspend fun createRoomAlias(createRoomAliasDTO: CreateRoomAliasDTO): RoomAlias? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()
        var error: PgException? = null

        val params = createRoomAliasDTO.params()
        println("params $params")

        val id = params.getString("id")
        val roomId = params.getString("room_id")
        val servers = params.getValue("servers")

        client.preparedQuery(CREATE_ROOM_ALIAS_QUERY).execute(Tuple.of(id, roomId, servers))
            .onSuccess { res ->
                run {
                    try {
                        result.complete(res.first().toJson())
                    } catch (e: NoSuchElementException) {
                        result.complete(null)
                    }
                }
            }
            .onFailure { err ->
                println(err)
                run {
                    error = PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                    result.fail(error);
                }
            }

        val resPayload = result.future().toCompletionStage().await() ?: return null

        return RoomAlias(resPayload)
    }

    override suspend fun getRoomAlias(getRoomAliasDTO: GetRoomAliasDTO): RoomAlias? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()
        var error: PgException? = null

        val params = getRoomAliasDTO.params()

        val roomAliasId = params.getString("roomAlias")

        client.preparedQuery(GET_ROOM_ALIAS_QUERY).execute(Tuple.of(roomAliasId)).onSuccess { res ->
            run {
                try {
                    result.complete(res.first().toJson())
                } catch (e: NoSuchElementException) {
                    result.complete(null)
                }
            }
        }
            .onFailure { err ->
                run {
                    error = PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                    result.fail(error);
                }
            }

        val resPayload = result.future().toCompletionStage().await() ?: return null

        return RoomAlias(resPayload)

    }

    override suspend fun getAliases(getAliasesDTO: GetAliasesDTO): List<RoomAlias>? {
        val client = database.client()
        val result: Promise<List<JsonObject>?> = Promise.promise()
        var error: PgException? = null

        val params = getAliasesDTO.params()

        val roomId = params.getString("roomId")

        client.preparedQuery(GET_ALIASES_QUERY).execute(Tuple.of(roomId)).onSuccess { res ->
            run {
                try {
                    result.complete(res.toList().map { it.toJson() })
                } catch (e: NoSuchElementException) {
                    result.complete(null)
                }
            }
        }
            .onFailure { err ->
                run {
                    error = PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                    result.fail(error);
                }
            }

        val resPayload = result.future().toCompletionStage().await() ?: return null

        return resPayload.map { RoomAlias(it) }
    }

    override suspend fun putRoomAlias(putRoomAliasDTO: PutRoomAliasDTO): RoomAlias? {
        val createRoomAliasDTO = CreateRoomAliasDTO(
            putRoomAliasDTO.params().apply { put("servers", arrayOf<String>(ResourceBundleConstants.domain)) })
        return createRoomAlias(createRoomAliasDTO)
    }

    override suspend fun deleteRoomAlias(deleteRoomAliasDTO: DeleteRoomAliasDTO): RoomAlias? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()
        var error: PgException? = null

        val params = deleteRoomAliasDTO.params()

        val roomAliasId = params.getString("roomAlias")

        client.preparedQuery(DELETE_ROOM_ALIAS_QUERY).execute(Tuple.of(roomAliasId)).onSuccess { res ->
            run {
                try {
                    result.complete(res.first().toJson())
                } catch (e: NoSuchElementException) {
                    result.complete(null)
                }
            }
        }
            .onFailure { err ->
                run {
                    error = PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                    result.fail(error);
                }
            }

        val resPayload = result.future().toCompletionStage().await()

        if (resPayload == null && error != null) {
            throw error!!
        }

        if (resPayload == null) {
            return null
        }

        return RoomAlias(resPayload)
    }


    companion object {
        val CREATE_ROOM_QUERY = ResourceBundleConstants.roomQueries.getString("createRoom")
        val DELETE_ROOM_QUERY = ResourceBundleConstants.roomQueries.getString("deleteRoom")
        val GET_ROOM_QUERY = ResourceBundleConstants.roomQueries.getString("findRoom")

        val CREATE_ROOM_ALIAS_QUERY = ResourceBundleConstants.roomQueries.getString("createRoomAlias")
        val GET_ROOM_ALIAS_QUERY = ResourceBundleConstants.roomQueries.getString("getRoomAlias")
        val GET_ALIASES_QUERY = ResourceBundleConstants.roomQueries.getString("getRoomAliases")
        val DELETE_ROOM_ALIAS_QUERY = ResourceBundleConstants.roomQueries.getString("deleteRoomAlias")
    }


}