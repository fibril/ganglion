package io.fibril.ganglion.clientServer.v1.roomEvents

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.CreateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgException
import kotlinx.coroutines.future.await


interface RoomEventService : Service<RoomEvent> {
    /**
     * Fetch events satisfying arbitrary map of params
     */
    suspend fun fetchEvents(conditionsMap: Map<String, String>): Future<List<RoomEvent>?>
    suspend fun getRoomMemberEvent(roomId: String, userId: String): RoomEvent?
    suspend fun getRoomPowerLevelEvent(roomId: String): RoomEvent?
    suspend fun getRoomJoinRuleEvent(roomId: String): RoomEvent?
    suspend fun createRoomEventsBatch(createEventDTOs: List<CreateRoomEventDTO>): Future<List<RoomEvent>>
}

class RoomEventServiceImpl @Inject constructor(
    private val roomEventRepository: RoomEventRepository,
    private val vertx: Vertx
) :
    RoomEventService {

    companion object {
        const val IDENTIFIER = "v1.rooms.RoomEventService"
    }

    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<RoomEvent> {
        val roomEvent = try {
            roomEventRepository.save(dto)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Exception",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }

        // notify creation of room members that needs their avatar_url and display name updated
        val eventBus = vertx.eventBus()
        if (roomEvent?.asJson()?.getString("type") == RoomEventNames.StateEvents.MEMBER) {
            eventBus.send(RoomEventDatabaseActions.ROOM_MEMBER_CREATED, roomEvent.asJson())
        }

        return Future.succeededFuture(roomEvent)
    }


    override suspend fun createRoomEventsBatch(createEventDTOs: List<CreateRoomEventDTO>): Future<List<RoomEvent>> {
        val roomEvents = mutableListOf<RoomEvent>()
        for (createEventDto in createEventDTOs) {
            try {
                val createdRoomEvent = create(createEventDto).toCompletionStage().await()
                if (createdRoomEvent != null) {
                    roomEvents.add(createdRoomEvent)
                }
            } catch (e: Exception) {
                //
                println("error creating roomEvent $e")
            }
        }

        return Future.succeededFuture(roomEvents)
    }


    override suspend fun findOne(id: String): Future<RoomEvent?> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(): Future<List<RoomEvent>> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Future<RoomEvent> {
        val roomEvent = try {
            roomEventRepository.update(id, dto)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Exception",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }

        return Future.succeededFuture(roomEvent)
    }

    override suspend fun remove(id: String): Future<Boolean> {
        TODO("Not yet implemented")
    }


    override suspend fun fetchEvents(conditionsMap: Map<String, String>): Future<List<RoomEvent>?> {
        val events = try {
            roomEventRepository.fetchEvents(conditionsMap)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Exception",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }

        return Future.succeededFuture(events)
    }

    override suspend fun getRoomMemberEvent(roomId: String, userId: String) =
        fetchEvents(
            mapOf(
                "type" to RoomEventNames.StateEvents.MEMBER,
                "room_id" to roomId,
                "state_key" to userId
            )
        ).toCompletionStage().await()?.firstOrNull()

    override suspend fun getRoomPowerLevelEvent(roomId: String) =
        fetchEvents(
            mapOf(
                "type" to RoomEventNames.StateEvents.POWER_LEVELS,
                "room_id" to roomId,
                "state_key" to RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY
            )
        ).toCompletionStage().await()?.firstOrNull()

    override suspend fun getRoomJoinRuleEvent(roomId: String) =
        fetchEvents(
            mapOf(
                "type" to RoomEventNames.StateEvents.JOIN_RULES,
                "room_id" to roomId,
                "state_key" to RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY
            )
        ).toCompletionStage().await()?.firstOrNull()

}