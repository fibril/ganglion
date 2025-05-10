package io.fibril.ganglion.clientServer.v1.roomEvents

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.CreateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.UpdateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.fibril.ganglion.clientServer.v1.rooms.RoomRepositoryImpl
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.await


interface RoomEventRepository : Repository<RoomEvent> {
    suspend fun fetchEvents(conditionsMap: Map<String, String>): List<RoomEvent>
}


class RoomEventRepositoryImpl @Inject constructor(private val database: PGDatabase) : RoomEventRepository {

    override suspend fun save(dto: DTO): RoomEvent {
        val params = dto.params()

        val pool = database.pool()
        val result: Promise<JsonObject?> = Promise.promise()
        var error: DatabaseException? = null

        var createRoomEventResponseRowSet: RowSet<Row>? = null

        pool.connection
            .onSuccess { conn ->
                conn.begin()
                    .compose { tx ->
                        conn.preparedQuery(CREATE_ROOM_EVENT_QUERY).execute(
                            Tuple.of(
                                params.getString("id"),
                                (dto as CreateRoomEventDTO).sender.principal().getString("sub"),
                                params.getString("room_id"),
                                JsonObject(params.getString("content")),
                                params.getString("state_key"),
                                params.getString("type"),
                            )
                        )
                            .compose { createdRoomEvent ->
                                createRoomEventResponseRowSet = createdRoomEvent
                                if (dto.roomEventName == RoomEventNames.StateEvents.CANONICAL_ALIAS) {
                                    val aliasId = params.getJsonObject("content").getString("alias")
                                    val roomId = createdRoomEvent.first().toJson().getString("room_id")
                                    val servers = arrayOf<String>(ResourceBundleConstants.domain)
                                    conn.preparedQuery(RoomRepositoryImpl.CREATE_ROOM_ALIAS_QUERY)
                                        .execute(Tuple.of(aliasId, roomId, servers))
                                } else {
                                    Future.succeededFuture(createdRoomEvent)
                                }
                            }.compose {
                                val txResponse = tx.commit()
                                val obj = RoomEvent(createRoomEventResponseRowSet!!.first().toJson()).asJson()
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

        return RoomEvent(resPayload!!)
    }

    override suspend fun find(id: String): RoomEvent? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()

        client.preparedQuery(FIND_ROOM_EVENT_QUERY).execute(Tuple.of(id)).onSuccess { res ->
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
                    throw PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                }
            }

        val resPayload = result.future().toCompletionStage().await() ?: return null

        return RoomEvent(resPayload)
    }


    override suspend fun findAll(query: String): List<RoomEvent> {
        val client = database.client()
        val result: Promise<List<RoomEvent>> = Promise.promise()
        client.query(query)
            .execute()
            .onSuccess { res ->
                result.complete(res.map { RoomEvent(it.toJson()) })
            }
            .onFailure { err ->
                throw PgException(
                    err.message,
                    "SEVERE",
                    "500",
                    err.message
                )
            }
            .eventually { _ -> client.close() }

        val roomEvents = result.future().toCompletionStage().await()
        return roomEvents
    }

    override suspend fun update(id: String, dto: DTO): RoomEvent? {
        val client = database.client()
        val result: Promise<RoomEvent> = Promise.promise()

        val updateRoomEventDTO = (dto as UpdateRoomEventDTO)

        val params = updateRoomEventDTO.params()
        val keys = params.map.keys
        val values = mutableListOf<Any>().apply {
            for (key in keys) {
                add("$key = '${params.getValue(key)}'")
            }
        }

        val queryString = """
                UPDATE room_events SET ${values.joinToString(", ")}
                WHERE id = '$id' 
                RETURNING *;
            """.trimIndent()

        client.query(
            queryString
        ).execute()
            .onSuccess { res ->
                val response = res.firstOrNull()?.toJson()
                result.complete(if (response != null) RoomEvent(response) else null)
            }
            .onFailure { err ->
                throw PgException(
                    err.message,
                    "SEVERE",
                    "500",
                    err.message
                )
            }
            .eventually { _ -> client.close() }

        val roomEvent = result.future().toCompletionStage().await()

        return roomEvent
    }

    override suspend fun delete(id: String): RoomEvent {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()
        var error: PgException? = null

        client.preparedQuery(DELETE_ROOM_EVENT_QUERY).execute(Tuple.of(id))
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

        return RoomEvent(resPayload!!)
    }

    override suspend fun fetchEvents(conditionsMap: Map<String, String>): List<RoomEvent> {
        val valuesQuery = mutableListOf<Any>().apply {
            for (key in conditionsMap.keys) {
                add("$key '${conditionsMap[key]}'")
            }
        }.joinToString(" AND ")

        val query = """
                SELECT * FROM room_events WHERE $valuesQuery;
            """.trimIndent()

        return findAll(query)
    }


    companion object {
        val CREATE_ROOM_EVENT_QUERY = ResourceBundleConstants.roomEventQueries.getString("createRoomEvent")
        val DELETE_ROOM_EVENT_QUERY = ResourceBundleConstants.roomEventQueries.getString("deleteRoomEvent")
        val FIND_ROOM_EVENT_QUERY = ResourceBundleConstants.roomEventQueries.getString("findRoomEvent")
    }


}