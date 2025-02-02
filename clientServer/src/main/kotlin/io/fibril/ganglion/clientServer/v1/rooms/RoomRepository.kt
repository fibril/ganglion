package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.rooms.dtos.CreateRoomAliasDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.CreateRoomDTO
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
}


class RoomRepositoryImpl @Inject constructor(private val database: PGDatabase) : RoomRepository {
    override suspend fun save(dto: DTO): Room {
        val createRoomDTO = dto as CreateRoomDTO
        val id = createRoomDTO.json.getString("id")
        val creatorId = createRoomDTO.sender?.principal()?.getString("sub")
        val isDirect = createRoomDTO.json.getString("is_direct") ?: false
//        val roomAliasName = createRoomDTO.json.getString("room_alias_name")
        val type = createRoomDTO.json.getString("type")
        val version = createRoomDTO.json.getString("room_version")
        val visibility = createRoomDTO.json.getString("visibility")

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
        TODO("Not yet implemented")
    }


    override suspend fun findAll(): List<Room> {
        TODO("Not yet implemented")
    }

    override suspend fun update(dto: DTO): Room? {
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

        val id = createRoomAliasDTO.json.getString("id")
        val roomId = createRoomAliasDTO.json.getString("room_id")
        val servers = createRoomAliasDTO.json.getString("servers")

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


    companion object {
        val CREATE_ROOM_QUERY = ResourceBundleConstants.roomQueries.getString("createRoom")
        val CREATE_ROOM_ALIAS_QUERY = ResourceBundleConstants.roomQueries.getString("createRoomAlias")
        val DELETE_ROOM_QUERY = ResourceBundleConstants.roomQueries.getString("deleteRoom")
    }


}