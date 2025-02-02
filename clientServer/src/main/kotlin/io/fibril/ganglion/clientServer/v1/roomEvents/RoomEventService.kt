package io.fibril.ganglion.clientServer.v1.roomEvents

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.vertx.core.Future


interface RoomEventService : Service<RoomEvent>

class RoomEventServiceImpl @Inject constructor(private val roomEventRepository: RoomEventRepository) :
    RoomEventService {

    companion object {
        const val IDENTIFIER = "v1.rooms.RoomEventService"
        const val DEFAULT_ROOM_VERSION = 11

        /**
         * Specifies the order of events that will be created, if applicable,
         * whenever a room is created
         */
        val roomCreationEventNamesInOrder = listOf(
            RoomEventNames.StateEvents.CREATE,
            RoomEventNames.StateEvents.MEMBER,
            RoomEventNames.StateEvents.POWER_LEVELS,
            RoomEventNames.StateEvents.CANONICAL_ALIAS,
            RoomEventNames.StateEvents.JOIN_RULES,
            RoomEventNames.StateEvents.HISTORY_VISIBILITY,
            RoomEventNames.StateEvents.NAME,
            RoomEventNames.StateEvents.TOPIC,
        )
    }

    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<RoomEvent> {
        TODO("Not yet implemented")
    }

    override suspend fun findOne(id: String): Future<RoomEvent> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(): Future<List<RoomEvent>> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Future<RoomEvent> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<Boolean> {
        TODO("Not yet implemented")
    }

}