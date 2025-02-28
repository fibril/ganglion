package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.only
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.RoomRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.rooms.dtos.*
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
            .authenticatedRoute(minimumRoleType = RoleType.USER)
            .handler(::createRoom)

        router.get(GET_JOINED_ROOMS_PATH)
            .authenticatedRoute(RoleType.USER)
            .handler(::getJoinedRooms)

        router.post(INVITE_USER_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(InviteUserDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::inviteUser)

        router.post(JOIN_VIA_ROOM_ID_OR_ALIAS_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(JoinRoomViaRoomIdOrAliasDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::joinViaRoomIdOrAlias)

        router.post(JOIN_VIA_ROOM_ID_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(JoinRoomViaRoomIdDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::joinViaRoomId)

        router.post(KNOCK_ROOM_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(KnockRoomDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::knockRoom)

        router.post(FORGET_ROOM_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(ForgetRoomDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::forgetRoom)

        router.post(LEAVE_ROOM_PATH)
            .addRequestRateLimiter(RoomRequestRateLimiter.getInstance())
            .useDTOValidation(LeaveRoomDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::leaveRoom)

        router.post(KICK_USER_PATH)
            .useDTOValidation(KickUserDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::kickUser)

        router.post(BAN_USER_PATH)
            .useDTOValidation(BanUserDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::banUser)

        router.post(UNBAN_USER_PATH)
            .useDTOValidation(UnBanUserDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::unBanUser)

        router.get(GET_ROOM_VISIBILITY_PATH).handler(::getRoomVisibility)

        router.put(PUT_ROOM_VISIBILITY_PATH)
            .useDTOValidation(UpdateRoomVisibilityDTO::class.java)
            .authenticatedRoute(RoleType.ADMIN)
            .handler(::updateRoomVisibility)

        router.get(GET_PUBLIC_ROOMS_PATH).handler(::getPublicRooms)
        router.post(GET_PUBLIC_ROOMS_PATH)
            .useDTOValidation(ListPublicRoomsDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::getPublicRooms)

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

    private fun getJoinedRooms(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            roomService.getJoinedRooms(routingContext.user())
                .onSuccess { roomEvents ->
                    val result = JsonObject().put("joined_rooms", roomEvents.map { it.asJson().getString("room_id") })
                    routingContext.end(result.toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun inviteUser(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val inviteUserDTO = InviteUserDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject()), routingContext.user()
            )
            roomService.inviteUser(inviteUserDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun joinViaRoomIdOrAlias(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val joinRoomViaRoomIdOrAliasDTO = JoinRoomViaRoomIdOrAliasDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject())
                    .mergeIn(JsonObject.mapFrom(routingContext.queryParams())), routingContext.user()
            )
            roomService.joinViaRoomIdOrAlias(joinRoomViaRoomIdOrAliasDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun joinViaRoomId(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val joinRoomViaRoomIdDTO = JoinRoomViaRoomIdDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject())
                    .mergeIn(JsonObject.mapFrom(routingContext.queryParams())), routingContext.user()
            )
            roomService.joinViaRoomId(joinRoomViaRoomIdDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun knockRoom(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val knockRoomDTO = KnockRoomDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject())
                    .mergeIn(JsonObject.mapFrom(routingContext.queryParams())), routingContext.user()
            )
            roomService.knockRoom(knockRoomDTO)
                .onSuccess {
                    routingContext.end(it.asJson().only("room_id").toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun leaveRoom(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val leaveRoomDTO = LeaveRoomDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject()), routingContext.user()
            )
            roomService.leaveRoom(leaveRoomDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun forgetRoom(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val forgetRoomDTO = ForgetRoomDTO(
                JsonObject.mapFrom(routingContext.pathParams()), routingContext.user()
            )
            roomService.forgetRoom(forgetRoomDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun kickUser(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val kickUserDTO = KickUserDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject()), routingContext.user()
            )
            roomService.kickUser(kickUserDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun banUser(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val banUserDTO = BanUserDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject()), routingContext.user()
            )
            roomService.banUser(banUserDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun unBanUser(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val unBanUserDTO = UnBanUserDTO(
                JsonObject.mapFrom(routingContext.pathParams())
                    .mergeIn(routingContext.body().asJsonObject()),
                routingContext.user()
            )
            roomService.unBanUser(unBanUserDTO)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun getRoomVisibility(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val roomId = routingContext.pathParams().getOrDefault("roomId", null) ?: run {
                routingContext.response().setStatusCode(400)
                routingContext.end(StandardErrorResponse(ErrorCodes.M_BAD_JSON).asJson().toString())
                return@usingCoroutineScopeWithIODispatcher
            }
            roomService.findOne(roomId)
                .onSuccess {
                    if (it == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                    } else {
                        routingContext.end(it.asJson().only("visibility").toString())
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun updateRoomVisibility(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val pathParams = routingContext.pathParams()
            val roomId = pathParams.get("roomId")
            val updateRoomVisibilityDTO = UpdateRoomVisibilityDTO(
                JsonObject.mapFrom(pathParams)
                    .mergeIn(routingContext.body().asJsonObject()),
                routingContext.user()
            )
            roomService.update(roomId!!, updateRoomVisibilityDTO)
                .onSuccess {
                    if (it == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                    } else {
                        routingContext.end(it.asJson().only("visibility").toString())
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun getPublicRooms(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val json = JsonObject().apply {
                for (entry in routingContext.queryParams()) {
                    put(entry.key, entry.value)
                }
            }.mergeIn(routingContext.body()?.asJsonObject() ?: JsonObject())
            val dto = ListPublicRoomsDTO(json)
            roomService.findAll(dto)
                .onSuccess {
                    routingContext.end(it.asJson().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }


    companion object {
        const val CREATE_ROOM_PATH = "/v3/createRoom"
        const val GET_JOINED_ROOMS_PATH = "/v3/joined_rooms"
        const val INVITE_USER_PATH = "/v3/rooms/:roomId/invite"
        const val JOIN_VIA_ROOM_ID_OR_ALIAS_PATH = "/v3/join/:roomIdOrAlias"
        const val JOIN_VIA_ROOM_ID_PATH = "/v3/rooms/:roomId/join"
        const val KNOCK_ROOM_PATH = "/v3/knock/:roomIdOrAlias"
        const val LEAVE_ROOM_PATH = "/v3/rooms/:roomId/leave"
        const val FORGET_ROOM_PATH = "/v3/rooms/:roomId/forget"
        const val KICK_USER_PATH = "/v3/rooms/:roomId/kick"
        const val BAN_USER_PATH = "/v3/rooms/:roomId/ban"
        const val UNBAN_USER_PATH = "/v3/rooms/:roomId/unban"
        const val GET_ROOM_VISIBILITY_PATH = "/v3/directory/list/room/:roomId"
        const val PUT_ROOM_VISIBILITY_PATH = "/v3/directory/list/room/:roomId"
        const val GET_PUBLIC_ROOMS_PATH = "/v3/publicRooms"
    }
}