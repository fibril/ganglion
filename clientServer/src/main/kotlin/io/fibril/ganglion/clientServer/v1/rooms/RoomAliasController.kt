package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.RoomRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.rooms.dtos.DeleteRoomAliasDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.GetAliasesDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.GetRoomAliasDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.PutRoomAliasDTO
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class RoomAliasController @Inject constructor(
    private val vertx: Vertx,
    private val roomAliasService: RoomAliasService
) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        router.get(GET_ROOM_ALIAS_PATH)
            .useDTOValidation(GetRoomAliasDTO::class.java)
            .handler(::getRoomAlias)

        router.get(GET_ALIASES_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(GetAliasesDTO::class.java)
            .authenticatedRoute(minimumRoleType = RoleType.USER)
            .handler(::getRoomAliases)

        router.route().handler(BodyHandler.create())
        router.put(PUT_ROOM_ALIAS_PATH)
            .useDTOValidation(PutRoomAliasDTO::class.java)
            .authenticatedRoute(minimumRoleType = RoleType.USER)
            .handler(::putRoomAlias)

        router.delete(DELETE_ROOM_ALIAS_PATH)
            .useDTOValidation(DeleteRoomAliasDTO::class.java)
            .authenticatedRoute(minimumRoleType = RoleType.USER)
            .handler(::deleteRoomAlias)

        return router
    }

    private fun getRoomAlias(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val getRoomAliasDTO = GetRoomAliasDTO(JsonObject.mapFrom(routingContext.pathParams()))
            roomAliasService.getRoomAlias(getRoomAliasDTO)
                .onSuccess { roomAlias ->
                    if (roomAlias == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(
                            StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString()
                        )
                    } else {
                        routingContext.end(
                            roomAlias.asJson().toString()
                        )
                    }
                }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun getRoomAliases(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val getRoomAliasesDTO =
                GetAliasesDTO(JsonObject.mapFrom(routingContext.pathParams()), routingContext.user())
            roomAliasService.getRoomAliases(getRoomAliasesDTO)
                .onSuccess { roomAliases ->
                    if (roomAliases == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(
                            StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString()
                        )
                    } else {
                        routingContext.end(
                            JsonObject().put("aliases", JsonArray(roomAliases.map { it.id })).toString()
                        )
                    }
                }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun putRoomAlias(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val putRoomAliasDTO = PutRoomAliasDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject())
            )
            roomAliasService.putRoomAlias(putRoomAliasDTO)
                .onSuccess { roomAlias ->
                    if (roomAlias == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(
                            StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString()
                        )
                    } else {
                        routingContext.end(
                            roomAlias.asJson().toString()
                        )
                    }
                }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun deleteRoomAlias(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val deleteRoomAliasDTO = DeleteRoomAliasDTO(
                JsonObject.mapFrom(routingContext.pathParams())
            )
            roomAliasService.deleteRoomAlias(deleteRoomAliasDTO)
                .onSuccess { roomAlias ->
                    if (roomAlias == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(
                            StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString()
                        )
                    } else {
                        routingContext.end(
                            roomAlias.asJson().toString()
                        )
                    }
                }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    companion object {
        const val GET_ROOM_ALIAS_PATH = "/v3/directory/room/:roomAlias"
        const val GET_ALIASES_PATH = "/v3/rooms/:roomId/aliases"
        const val PUT_ROOM_ALIAS_PATH = "/v3/directory/room/:roomAlias"
        const val DELETE_ROOM_ALIAS_PATH = "/v3/directory/room/:roomAlias"
    }
}