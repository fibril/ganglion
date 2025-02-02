package io.fibril.ganglion.clientServer.v1.roomEvents

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.CreateRoomEventDTO
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


interface RoomEventRepository : Repository<RoomEvent>


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
                                params.getString("content"),
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
        TODO("Not yet implemented")
    }


    override suspend fun findAll(): List<RoomEvent> {
        TODO("Not yet implemented")
    }

    override suspend fun update(dto: DTO): RoomEvent? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): RoomEvent? {
        TODO("Not yet implemented")
    }


    companion object {
        val CREATE_ROOM_EVENT_QUERY = ResourceBundleConstants.roomEventQueries.getString("createRoomEvent")
    }


}