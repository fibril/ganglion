package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRouteForUser
import io.fibril.ganglion.clientServer.extensions.only
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.RoomRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.rooms.dtos.CreateRoomDTO
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class RoomController @Inject constructor(private val vertx: Vertx, private val roomService: RoomService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        router.route().handler(BodyHandler.create())
        router.post(CREATE_ROOM_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(CreateRoomDTO::class.java)
            .authenticatedRouteForUser()
            .handler(::createRoom)

        return router
    }

    private fun createRoom(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val createRoomDTO =
                CreateRoomDTO(routingContext.body()?.asJsonObject() ?: JsonObject(), sender = routingContext.user())
            roomService.create(createRoomDTO)
                .onSuccess { room ->
                    routingContext.end(
                        room.asJson().apply {
                            put("room_id", getString("id"))
                        }
                            .only("room_id").toString())
                }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    companion object {
        const val CREATE_ROOM_PATH = "/v3/createRoom"
    }
}