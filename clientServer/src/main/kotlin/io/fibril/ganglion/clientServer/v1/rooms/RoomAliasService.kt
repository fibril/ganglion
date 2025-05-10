package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventDatabaseActions
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.rooms.dtos.DeleteRoomAliasDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.GetAliasesDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.GetRoomAliasDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.PutRoomAliasDTO
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAlias
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAliasId
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import kotlinx.coroutines.future.await


interface RoomAliasService : Service<RoomAlias> {
    suspend fun getRoomAlias(getRoomAliasDTO: GetRoomAliasDTO): Future<RoomAlias?>
    suspend fun getRoomAliases(getRoomAliasesDTO: GetAliasesDTO): Future<List<RoomAlias>?>
    suspend fun putRoomAlias(putRoomAliasDTO: PutRoomAliasDTO): Future<RoomAlias?>
    suspend fun deleteRoomAlias(deleteRoomAliasDTO: DeleteRoomAliasDTO): Future<RoomAlias?>
}

class RoomAliasServiceImpl @Inject constructor(
    private val roomAliasRepository: RoomAliasRepository,
    private val roomEventService: RoomEventService,
    private val vertx: Vertx
) : RoomAliasService {

    companion object {
        const val IDENTIFIER = "v1.rooms.RoomAliasService"
    }

    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<RoomAlias> {
        val roomAlias = try {
            roomAliasRepository.save(dto)
        } catch (e: Exception) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }
        vertx.eventBus().send(RoomEventDatabaseActions.ROOM_ALIAS_CREATED, roomAlias!!.asJson())
        return Future.succeededFuture(roomAlias)
    }

    override suspend fun getRoomAlias(getRoomAliasDTO: GetRoomAliasDTO): Future<RoomAlias?> {
        val roomAlias = try {
            roomAliasRepository.getRoomAlias(getRoomAliasDTO)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }
        return Future.succeededFuture(roomAlias)
    }

    override suspend fun getRoomAliases(getRoomAliasesDTO: GetAliasesDTO): Future<List<RoomAlias>?> {
        val fetchUserMembershipEventsConditionsMap = mapOf<String, String>(
            "state_key =" to (getRoomAliasesDTO.sender?.principal()?.getString("sub") ?: "----"),
            "type =" to RoomEventNames.StateEvents.MEMBER,
            "room_id =" to getRoomAliasesDTO.params().getString("roomId")
        )

        val membershipEvents = try {
            roomEventService.fetchEvents(fetchUserMembershipEventsConditionsMap).toCompletionStage().await()
        } catch (e: Exception) {
            return Future.failedFuture(
                RequestException(
                    409,
                    e.message ?: "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_FORBIDDEN).asJson()
                )
            )
        }

        val fetchRoomHistoryVisibilityEventConditionsMap = mapOf<String, String>(
            "type =" to RoomEventNames.StateEvents.HISTORY_VISIBILITY,
            "room_id =" to getRoomAliasesDTO.params().getString("roomId")
        )

        val historyVisibilityEvent = try {
            roomEventService.fetchEvents(fetchRoomHistoryVisibilityEventConditionsMap).toCompletionStage().await()
                ?.first()
        } catch (e: Exception) {
            return Future.failedFuture(
                RequestException(
                    403,
                    e.message ?: "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_FORBIDDEN).asJson()
                )
            )
        }

        val canReadAliases =
            membershipEvents?.find {
                JsonObject(
                    it.asJson().getString("content")
                ).getString("membership") == "join"
            } != null ||
                    JsonObject(historyVisibilityEvent?.asJson()?.getString("content"))
                        .getString("history_visibility") == "world_readable"

        if (!canReadAliases) {
            return Future.failedFuture(
                RequestException(
                    403,
                    "",
                    StandardErrorResponse(ErrorCodes.M_FORBIDDEN).asJson()
                )
            )
        }

        val aliases = try {
            roomAliasRepository.getAliases(getRoomAliasesDTO)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }
        return Future.succeededFuture(aliases)
    }

    override suspend fun putRoomAlias(putRoomAliasDTO: PutRoomAliasDTO): Future<RoomAlias?> {
        try {
            val roomAliasCheck = roomAliasRepository.getRoomAlias(
                GetRoomAliasDTO(
                    JsonObject().put(
                        "roomAlias",
                        putRoomAliasDTO.params().getString("id")
                    )
                )
            )
            if (roomAliasCheck != null) {
                return Future.failedFuture(
                    RequestException(
                        409,
                        "",
                        StandardErrorResponse(ErrorCodes.M_ALIAS_IN_USE, "Alias Already Exists").asJson()
                    )
                )
            }
        } catch (e: Exception) {
            // ignore
        }
        val roomAlias = try {
            roomAliasRepository.putRoomAlias(putRoomAliasDTO)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }

        vertx.eventBus().send(RoomEventDatabaseActions.ROOM_ALIAS_UPDATED, roomAlias!!.asJson())

        return Future.succeededFuture(roomAlias)
    }

    override suspend fun deleteRoomAlias(deleteRoomAliasDTO: DeleteRoomAliasDTO): Future<RoomAlias?> {
        val deletedRoomAlias = try {
            roomAliasRepository.deleteRoomAlias(deleteRoomAliasDTO)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }
        if (deletedRoomAlias == null) {
            return Future.failedFuture(
                RequestException(
                    404,
                    "",
                    StandardErrorResponse(
                        ErrorCodes.M_NOT_FOUND,
                        "Room alias ${deleteRoomAliasDTO.params().getValue("roomAlias")} not found."
                    ).asJson()
                )
            )
        }

        vertx.eventBus().send(RoomEventDatabaseActions.ROOM_ALIAS_DELETED, deletedRoomAlias.asJson())

        return Future.succeededFuture(deletedRoomAlias)
    }


    override suspend fun findOne(id: String): Future<RoomAlias?> {
        val roomAliasId = try {
            RoomAliasId(id)
        } catch (e: IllegalStateException) {
            return Future.failedFuture(
                RequestException(
                    400,
                    e.message!!,
                    StandardErrorResponse(ErrorCodes.M_BAD_JSON, e.message).asJson()
                )
            )
        }
        return getRoomAlias(GetRoomAliasDTO(json = JsonObject.of("roomAlias", roomAliasId.toString())))
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<RoomAlias>> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Future<RoomAlias> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<RoomAlias> {
        TODO("Not yet implemented")
    }

}