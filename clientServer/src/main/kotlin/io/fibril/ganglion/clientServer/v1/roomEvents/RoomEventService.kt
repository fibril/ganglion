package io.fibril.ganglion.clientServer.v1.roomEvents

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.vertx.core.Future
import io.vertx.pgclient.PgException


interface RoomEventService : Service<RoomEvent> {
    /**
     * Fetch events satisfying arbitrary map of params
     */
    suspend fun fetchEvents(conditionsMap: Map<String, String>): Future<List<RoomEvent>?>
}

class RoomEventServiceImpl @Inject constructor(private val roomEventRepository: RoomEventRepository) :
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

        return Future.succeededFuture(roomEvent)
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

}