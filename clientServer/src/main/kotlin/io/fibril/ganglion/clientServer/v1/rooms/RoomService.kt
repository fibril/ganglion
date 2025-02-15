package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.exclude
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventUtils
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.CreateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.UpdateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.fibril.ganglion.clientServer.v1.rooms.dtos.*
import io.fibril.ganglion.clientServer.v1.rooms.models.Room
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAlias
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException
import kotlinx.coroutines.future.await
import io.vertx.ext.auth.User as VertxUser


interface RoomService : Service<Room> {
    suspend fun getJoinedRooms(vertxUser: VertxUser): Future<List<RoomEvent>>
    suspend fun inviteUser(inviteUserDTO: InviteUserDTO): Future<RoomEvent>
    suspend fun joinViaRoomIdOrAlias(joinRoomViaRoomIdOrAliasDTO: JoinRoomViaRoomIdOrAliasDTO): Future<JsonObject>
    suspend fun joinViaRoomId(joinRoomViaRoomIdDTO: JoinRoomViaRoomIdDTO): Future<JsonObject>
    suspend fun knockRoom(knockRoomDTO: KnockRoomDTO): Future<JsonObject>
    suspend fun leaveRoom(leaveRoomDTO: LeaveRoomDTO): Future<JsonObject>
    suspend fun forgetRoom(forgetRoomDTO: ForgetRoomDTO): Future<JsonObject>


    suspend fun canJoinRoom(roomId: String, userId: String): Future<Boolean>
    suspend fun canKick(roomId: String, roomMemberId: String, anotherMemberId: String): Future<Boolean>
    suspend fun canBan(roomId: String, roomMemberId: String, anotherMemberId: String): Future<Boolean>
    suspend fun canInvite(roomId: String, roomMemberId: String, anotherMemberId: String): Future<Boolean>
    suspend fun canKnockRoom(roomId: String, userId: String): Future<Boolean>

}

class RoomServiceImpl @Inject constructor(
    private val roomRepository: RoomRepository,
    private val roomEventService: RoomEventService,
    private val roomAliasService: RoomAliasService,
    private val vertx: Vertx
) : RoomService {

    companion object {
        const val IDENTIFIER = "v1.rooms.RoomService"
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

    override suspend fun create(dto: DTO): Future<Room> {
        // ensure an id into the dto
        val jsonWithId = dto.params().apply {
            if (getString("id") == null) {
                put("id", Utils.generateRoomId())
            }
        }

        val createRoomDTO = CreateRoomDTO(jsonWithId, dto.sender)
        val createRoomJson = createRoomDTO.params()

        val roomId = createRoomJson.getString("id")

        val roomVersion = createRoomJson.getString("room_version")
        if (roomVersion != null && roomVersion.toInt() > DEFAULT_ROOM_VERSION) {
            return Future.failedFuture(
                RequestException(
                    statusCode = 400,
                    ErrorCodes.M_UNSUPPORTED_ROOM_VERSION.name,
                    StandardErrorResponse(ErrorCodes.M_UNSUPPORTED_ROOM_VERSION).asJson()
                )
            )
        }

        val eventsToCreate = try {
            RoomUtils.generateEventsForRoomCreation(createRoomDTO)
        } catch (err: RequestException) {
            return Future.failedFuture(err)
        }

        val room = try {
            roomRepository.save(CreateRoomDTO(createRoomJson, dto.sender))
        } catch (e: DatabaseException) {
            return Future.failedFuture(
                RequestException(
                    statusCode = 500,
                    e.message ?: ErrorCodes.M_UNKNOWN.name,
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN, e.message).asJson()
                )
            )
        }

        val createdRoomEvents = roomEventService.createRoomEventsBatch(eventsToCreate).toCompletionStage().await()

        if (createdRoomEvents.size == eventsToCreate.size) {
            // all events were created successfully
            return Future.succeededFuture(room)
        } else {
            // some roomEvents failed
            // TODO:- Determine which failed
            val failedEventNames = mutableListOf<String>().apply {
                for (event in eventsToCreate) {
                    if (createdRoomEvents.find { roomEvent ->
                            roomEvent.id == event.params().getString("id")
                        } == null) {
                        add(event.roomEventName)
                    }
                }
            }
            // delete the room and the events will be cascade deleted too
            remove(roomId).toCompletionStage().await()
            return Future.failedFuture(
                RequestException(
                    statusCode = 500,
                    ErrorCodes.M_UNKNOWN.name,
                    StandardErrorResponse(
                        errCode = ErrorCodes.M_UNKNOWN,
                        error = "Unable to create room events: ${failedEventNames.joinToString(", ")}"
                    ).asJson()
                )
            )
        }

    }

    override suspend fun getJoinedRooms(vertxUser: User): Future<List<RoomEvent>> {
        val joinedRooms = try {
            roomEventService.fetchEvents(
                mapOf(
                    "state_key" to vertxUser.principal().getString("sub"),
                    "type" to RoomEventNames.StateEvents.MEMBER
                )
            ).toCompletionStage().await()
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }

        return Future.succeededFuture((joinedRooms ?: listOf()).filter {
            JsonObject(
                it.asJson().getString("content")
            ).getString("membership") == RoomMembershipState.JOIN.name.lowercase()
        })
    }

    override suspend fun findOne(id: String): Future<Room?> {
        val room = try {
            roomRepository.find(id)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        return Future.succeededFuture(room)
    }

    override suspend fun findAll(): Future<List<Room>> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Future<Room> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<Boolean> {
        val deletedRoom = try {
            roomRepository.delete(id)
        } catch (e: PgException) {
            return Future.succeededFuture(false)
        }
        return Future.succeededFuture(deletedRoom != null)
    }

    override suspend fun inviteUser(inviteUserDTO: InviteUserDTO): Future<RoomEvent> {
        val params = inviteUserDTO.params()
        val senderId = inviteUserDTO.sender?.principal()?.getString("sub") ?: ""
        val roomId = params.getString("roomId")
        val userId = params.getString("user_id")
        val canDoInvitation = canInvite(roomId, senderId, userId).toCompletionStage().await()
        if (canDoInvitation) {
            val inviteeRoomEvent = roomEventService.getRoomMemberEvent(roomId, userId)
            if (inviteeRoomEvent == null) {
                // create membership
                val createRoomEventDTO = CreateRoomEventDTO(
                    json = JsonObject().apply {
                        put("id", RoomEventUtils.generateEventId())
                        put(
                            "content",
                            JsonObject.of(
                                "power_level", 0,
                                "avatar_url", null, // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                                "displayname", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                                "display_name", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                                "is_direct", false,
                                "membership", "invite"
                            )
                        )
                            .put("sender", senderId)
                            .put("type", RoomEventNames.StateEvents.MEMBER)
                            .put("state_key", userId)
                            .put("room_id", roomId)
                    },
                    roomEventName = RoomEventNames.StateEvents.MEMBER,
                    sender = inviteUserDTO.sender!!
                )
                return roomEventService.create(createRoomEventDTO)
            }
            if (inviteeRoomEvent.asJson().getString("state_key") == senderId) {
                return Future.failedFuture(
                    RequestException(
                        403,
                        "Member invite denied",
                        StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "You cannot invite yourself").asJson()
                    )
                )
            }
            val currentMembershipState =
                JsonObject(inviteeRoomEvent.asJson().getString("content"))?.getString("membership") ?: ""
            val roomMembershipState =
                RoomMembership.mapNameToMembershipState.get(currentMembershipState) ?: return Future.failedFuture(
                    RequestException(
                        400,
                        "Cannot transition membership null to invite",
                        StandardErrorResponse(ErrorCodes.M_BAD_JSON).asJson()
                    )
                )
            if (RoomMembership.transitionGraph.hasPathFrom(roomMembershipState) to RoomMembershipState.INVITE) {
                // update membership
                val updateRoomEventDTO = UpdateRoomEventDTO(
                    json = JsonObject().apply {
                        put(
                            "content",
                            JsonObject.of(
                                "membership", RoomMembershipState.INVITE.name.lowercase()
                            ).mergeIn(JsonObject(inviteeRoomEvent.asJson().getString("content")).exclude("membership"))
                        )
                    },
                    roomEventName = RoomEventNames.StateEvents.MEMBER,
                    sender = inviteUserDTO.sender!!
                )
                return roomEventService.update(inviteeRoomEvent.id, updateRoomEventDTO)
            }
        }
        return Future.failedFuture(
            RequestException(
                403,
                "Member invite denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Member invite denied").asJson()
            )
        )
    }

    override suspend fun joinViaRoomIdOrAlias(joinRoomViaRoomIdOrAliasDTO: JoinRoomViaRoomIdOrAliasDTO): Future<JsonObject> {
        val params = joinRoomViaRoomIdOrAliasDTO.params()
        val vertxUser = joinRoomViaRoomIdOrAliasDTO.sender ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )
        val joinerId = vertxUser.principal().getString("sub")
        val aliasOrRoomId = params.getString("roomIdOrAlias")

        val roomId = resolveRoomIdFromRoomIdOrAliasString(aliasOrRoomId) ?: return Future.failedFuture(
            RequestException(
                403,
                "Permission denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Cannot resolve room_id from roomIdOrAlias").asJson()
            )
        )
        val canJoin = canJoinRoom(roomId, joinerId).toCompletionStage().await()
        if (canJoin) {
            return joinRoom(roomId, vertxUser)
        }
        return Future.failedFuture(
            RequestException(
                403,
                "Permission denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Permission denied").asJson()
            )
        )
    }

    override suspend fun joinViaRoomId(joinRoomViaRoomIdDTO: JoinRoomViaRoomIdDTO): Future<JsonObject> {
        val params = joinRoomViaRoomIdDTO.params()
        val vertxUser = joinRoomViaRoomIdDTO.sender ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )
        val joinerId = vertxUser.principal().getString("sub")
        val roomId = params.getString("roomId")
        val canJoin = canJoinRoom(roomId, joinerId).toCompletionStage().await()
        if (canJoin) {
            return joinRoom(roomId, vertxUser)
        }
        return Future.failedFuture(
            RequestException(
                403,
                "Permission denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Permission denied").asJson()
            )
        )
    }

    override suspend fun knockRoom(knockRoomDTO: KnockRoomDTO): Future<JsonObject> {
        val params = knockRoomDTO.params()
        val vertxUser = knockRoomDTO.sender ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )
        val knockerId = vertxUser.principal().getString("sub")
        val aliasOrRoomId = params.getString("roomIdOrAlias")

        val roomId = resolveRoomIdFromRoomIdOrAliasString(aliasOrRoomId) ?: return Future.failedFuture(
            RequestException(
                403,
                "Permission denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Cannot resolve room_id from roomIdOrAlias").asJson()
            )
        )
        val canKnock = canKnockRoom(roomId, knockerId).toCompletionStage().await()
        if (canKnock) {
            return knock(roomId, vertxUser)
        }
        return Future.failedFuture(
            RequestException(
                403,
                "Permission denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Permission denied").asJson()
            )
        )
    }

    override suspend fun leaveRoom(leaveRoomDTO: LeaveRoomDTO): Future<JsonObject> {
        val params = leaveRoomDTO.params()
        val vertxUser = leaveRoomDTO.sender
        val userId = vertxUser?.principal()?.getString("sub") ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )

        val roomId = params.getString("roomId")
        val leaverRoomEvent = roomEventService.getRoomMemberEvent(roomId, userId) ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(
                    ErrorCodes.M_BAD_JSON,
                    "User is not a member of the room with id ${roomId}"
                ).asJson()
            )
        )

        val updateRoomEventDTO = UpdateRoomEventDTO(
            json = JsonObject().apply {
                put(
                    "content",
                    JsonObject.of(
                        "membership", RoomMembershipState.LEAVE.name.lowercase(),
                        "power_level", 0
                    ).mergeIn(
                        JsonObject(
                            leaverRoomEvent.asJson().getString("content")
                        ).exclude("membership", "power_level")
                    )
                )
            },
            roomEventName = RoomEventNames.StateEvents.MEMBER,
            sender = vertxUser
        )

        val leftPromise = Promise.promise<Boolean>()
        try {
            roomEventService.update(leaverRoomEvent.id, updateRoomEventDTO).onSuccess {
                leftPromise.complete(true)
            }.onFailure {
                leftPromise.complete(false)
            }
        } catch (e: RequestException) {
            return Future.failedFuture(e)
        }
        val left = leftPromise.future().toCompletionStage().await()
        return if (left)
            Future.succeededFuture(JsonObject.of("room_id", roomId))
        else Future.failedFuture(
            RequestException(
                500,
                "Unknown Error",
                StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
            )
        )
    }

    override suspend fun forgetRoom(forgetRoomDTO: ForgetRoomDTO): Future<JsonObject> {
        val params = forgetRoomDTO.params()
        val vertxUser = forgetRoomDTO.sender
        val userId = vertxUser?.principal()?.getString("sub") ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )

        val roomId = params.getString("roomId")
        val leaverRoomEvent = roomEventService.getRoomMemberEvent(roomId, userId) ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(
                    ErrorCodes.M_BAD_JSON,
                    "User is not a member of the room with id ${roomId}"
                ).asJson()
            )
        )

        val forgotPromise = Promise.promise<Boolean>()
        try {
            roomEventService.remove(leaverRoomEvent.id).onSuccess {
                forgotPromise.complete(true)
            }.onFailure {
                forgotPromise.complete(false)
            }
        } catch (e: RequestException) {
            return Future.failedFuture(e)
        }
        val forgot = forgotPromise.future().toCompletionStage().await()
        return if (forgot)
            Future.succeededFuture(JsonObject.of("room_id", roomId))
        else Future.failedFuture(
            RequestException(
                500,
                "Unknown Error",
                StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
            )
        )
    }


    // PERMISSIONS

    override suspend fun canJoinRoom(roomId: String, userId: String): Future<Boolean> {
        val joinRuleEvent = roomEventService.getRoomJoinRuleEvent(roomId) ?: return Future.succeededFuture(false)
        val content = JsonObject(joinRuleEvent.asJson().getString("content"))
        val joinRule = content.getString("join_rule")
        var canJoinRoom = false
        when (joinRule) {
            "public" -> {
                canJoinRoom = true
            }

            "restricted" -> {
                val allowedRoomIds =
                    (JsonArray(content.getString("allow")).list as List<JsonObject>).map { it.getString("room_id") }
                        .toSet()
                val userMembershipEvents = (roomEventService.fetchEvents(
                    mapOf(
                        "state_key" to userId,
                        "type" to RoomEventNames.StateEvents.MEMBER
                    )
                ).toCompletionStage().await() ?: listOf()).filter {
                    JsonObject(it.asJson().getString("content")).getString("membership") == "join"
                }.map { it.asJson().getString("room_id") }

                canJoinRoom =
                    userMembershipEvents.isNotEmpty() &&
                            userMembershipEvents.any { it in allowedRoomIds }

            }

            else -> canJoinRoom = false
        }

        if (canJoinRoom) {
            // validate user is not banned
            val userMembership = roomEventService.getRoomMemberEvent(roomId, userId)
            if (userMembership != null && JsonObject(
                    userMembership.asJson().getString("content") ?: JsonObject().toString()
                ).getString("membership") == RoomMembershipState.BAN.name.lowercase()
            ) {
                return Future.succeededFuture(false)
            }
        }

        return Future.succeededFuture(canJoinRoom)

    }

    override suspend fun canKick(roomId: String, roomMemberId: String, anotherMemberId: String): Future<Boolean> {
        return canDo("kick", roomId, roomMemberId, anotherMemberId)
    }

    override suspend fun canBan(roomId: String, roomMemberId: String, anotherMemberId: String): Future<Boolean> {
        return canDo("ban", roomId, roomMemberId, anotherMemberId)
    }

    override suspend fun canInvite(roomId: String, roomMemberId: String, anotherMemberId: String): Future<Boolean> {
        return canDo("invite", roomId, roomMemberId, anotherMemberId)
    }

    override suspend fun canKnockRoom(roomId: String, userId: String): Future<Boolean> {
        val joinRuleEvent = roomEventService.getRoomJoinRuleEvent(roomId) ?: return Future.succeededFuture(false)
        val content = JsonObject(joinRuleEvent.asJson().getString("content"))
        val joinRule = content.getString("join_rule")
        var canKnockRoom = false
        when (joinRule) {
            "knock" -> {
                canKnockRoom = true
            }

            "knock_restricted" -> {
                val allowedRoomIds =
                    (JsonArray(content.getString("allow")).list as List<JsonObject>).map { it.getString("room_id") }
                        .toSet()
                val userMembershipEvents = (roomEventService.fetchEvents(
                    mapOf(
                        "state_key" to userId,
                        "type" to RoomEventNames.StateEvents.MEMBER
                    )
                ).toCompletionStage().await() ?: listOf()).filter {
                    JsonObject(it.asJson().getString("content")).getString("membership") == "join"
                }.map { it.asJson().getString("room_id") }

                canKnockRoom =
                    userMembershipEvents.isNotEmpty() &&
                            userMembershipEvents.any { it in allowedRoomIds }

            }

            else -> canKnockRoom = false
        }

        if (canKnockRoom) {
            // validate user is not banned
            val userMembership = roomEventService.getRoomMemberEvent(roomId, userId)
            if (userMembership != null && JsonObject(
                    userMembership.asJson().getString("content") ?: JsonObject().toString()
                ).getString("membership") == RoomMembershipState.BAN.name.lowercase()
            ) {
                return Future.succeededFuture(false)
            }
        }

        return Future.succeededFuture(canKnockRoom)

    }


    fun canRedact(roomId: String, roomMember: RoomEvent, anotherMember: RoomEvent): Future<Boolean> {
        return Future.succeededFuture(true)
    }

    private suspend fun joinRoom(roomId: String, vertxUser: User): Future<JsonObject> {
        val joinerId = vertxUser.principal()?.getString("sub") ?: return Future.failedFuture(
            RequestException(
                401,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )
        // if the user already has an invitation, upgrade it to join
        val createdPromise = Promise.promise<Boolean>()
        val inviteeRoomEvent = roomEventService.getRoomMemberEvent(roomId, joinerId)
        val powerLevelEvent = roomEventService.getRoomPowerLevelEvent(roomId)
        val defaultPowerLevelForJoiners = JsonObject(
            powerLevelEvent?.asJson()?.getString("content") ?: JsonObject().toString()
        ).getString("state_default") ?: 0
        if (inviteeRoomEvent == null) {
            // create membership
            val createRoomEventDTO = CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    put(
                        "content",
                        JsonObject.of(
                            "power_level", defaultPowerLevelForJoiners,
                            "avatar_url", null, // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "displayname", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "display_name", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "is_direct", false,
                            "membership", "invite"
                        )
                    )
                        .put("sender", joinerId)
                        .put("type", RoomEventNames.StateEvents.MEMBER)
                        .put("state_key", joinerId)
                        .put("room_id", roomId)
                },
                roomEventName = RoomEventNames.StateEvents.MEMBER,
                sender = vertxUser
            )

            try {
                roomEventService.create(createRoomEventDTO).onSuccess {
                    createdPromise.complete(true)
                }.onFailure {
                    createdPromise.complete(false)
                }
            } catch (e: RequestException) {
                return Future.failedFuture(e)
            }
        } else {
            // update the event to join
            val updateRoomEventDTO = UpdateRoomEventDTO(
                json = JsonObject().apply {
                    put(
                        "content",
                        JsonObject.of(
                            "membership", RoomMembershipState.JOIN.name.lowercase(),
                            "power_level", defaultPowerLevelForJoiners
                        ).mergeIn(
                            JsonObject(
                                inviteeRoomEvent.asJson().getString("content")
                            ).exclude("membership", "power_level")
                        )
                    )
                },
                roomEventName = RoomEventNames.StateEvents.MEMBER,
                sender = vertxUser
            )

            try {
                roomEventService.update(inviteeRoomEvent.id, updateRoomEventDTO).onSuccess {
                    createdPromise.complete(true)
                }.onFailure {
                    createdPromise.complete(false)
                }
            } catch (e: RequestException) {
                return Future.failedFuture(e)
            }
        }
        val created = createdPromise.future().toCompletionStage().await()
        return if (created) {
            Future.succeededFuture(JsonObject().put("room_id", roomId))
        } else {
            Future.failedFuture(
                RequestException(
                    500,
                    "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }
    }

    private suspend fun knock(roomId: String, vertxUser: User): Future<JsonObject> {
        val knockerId = vertxUser.principal()?.getString("sub") ?: return Future.failedFuture(
            RequestException(
                401,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )
        // if the user already has an invitation, upgrade it to join
        val createdPromise = Promise.promise<Boolean>()
        val knockerRoomEvent = roomEventService.getRoomMemberEvent(roomId, knockerId)
        val powerLevelEvent = roomEventService.getRoomPowerLevelEvent(roomId)
        val defaultPowerLevelForKnockers = JsonObject(
            powerLevelEvent?.asJson()?.getString("content") ?: JsonObject().toString()
        ).getString("users_default") ?: 0
        if (knockerRoomEvent == null) {
            // create membership
            val createRoomEventDTO = CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    put(
                        "content",
                        JsonObject.of(
                            "power_level", defaultPowerLevelForKnockers,
                            "avatar_url", null, // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "displayname", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "display_name", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "is_direct", false,
                            "membership", RoomMembershipState.KNOCK.name.lowercase()
                        )
                    )
                        .put("sender", knockerId)
                        .put("type", RoomEventNames.StateEvents.MEMBER)
                        .put("state_key", knockerId)
                        .put("room_id", roomId)
                },
                roomEventName = RoomEventNames.StateEvents.MEMBER,
                sender = vertxUser
            )

            try {
                roomEventService.create(createRoomEventDTO).onSuccess {
                    createdPromise.complete(true)
                }.onFailure {
                    createdPromise.complete(false)
                }
            } catch (e: RequestException) {
                return Future.failedFuture(e)
            }
        } else if (JsonObject(
                knockerRoomEvent.asJson().getString("content") ?: JsonObject().toString()
            ).getString("membership") == RoomMembershipState.LEAVE.name.lowercase()
        ) {
            // Update to knock if user had previously left the room
            val updateRoomEventDTO = UpdateRoomEventDTO(
                json = JsonObject().apply {
                    put(
                        "content",
                        JsonObject.of(
                            "membership", RoomMembershipState.KNOCK.name.lowercase(),
                            "power_level", defaultPowerLevelForKnockers
                        ).mergeIn(
                            JsonObject(
                                knockerRoomEvent.asJson().getString("content")
                            ).exclude("membership", "power_level")
                        )
                    )
                },
                roomEventName = RoomEventNames.StateEvents.MEMBER,
                sender = vertxUser
            )

            try {
                roomEventService.update(knockerRoomEvent.id, updateRoomEventDTO).onSuccess {
                    createdPromise.complete(true)
                }.onFailure {
                    createdPromise.complete(false)
                }
            } catch (e: RequestException) {
                return Future.failedFuture(e)
            }
        } else {
            createdPromise.complete(false)
        }

        val created = createdPromise.future().toCompletionStage().await()
        return if (created) {
            Future.succeededFuture(JsonObject().put("room_id", roomId))
        } else {
            Future.failedFuture(
                RequestException(
                    500,
                    "Unknown Error",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }
    }


    private suspend fun canDo(
        action: String,
        roomId: String,
        roomMemberId: String,
        anotherMemberId: String
    ): Future<Boolean> {
        val powerLevels = roomEventService.getRoomPowerLevelEvent(roomId)

        val roomMemberEvent = roomEventService.getRoomMemberEvent(roomId, roomMemberId)

        val anotherMemberEvent = roomEventService.getRoomMemberEvent(roomId, anotherMemberId)


        return Future.succeededFuture(
            powerLevels != null &&
                    roomMemberEvent != null &&
                    comparePowerLevel(
                        JsonObject(powerLevels.asJson().getString("content")).getInteger(action) ?: 0,
                        JsonObject(roomMemberEvent.asJson().getString("content"))
                            .getInteger("power_level") ?: 0,
                        JsonObject(anotherMemberEvent?.asJson()?.getString("content") ?: JsonObject().toString())
                            .getInteger("power_level") ?: 0,
                    )
        )
    }

    /**
     * Compares two power levels using a reference.
     * Determines if memberOne can perform an action that impacts memberTwo
     */
    private fun comparePowerLevel(
        referencePowerLevel: Int,
        memberOnePowerLevel: Int,
        memberTwoPowerLevel: Int
    ): Boolean {
        return memberOnePowerLevel >= referencePowerLevel && memberOnePowerLevel >= memberTwoPowerLevel
    }

    private suspend fun resolveRoomIdFromRoomIdOrAliasString(roomIdOrAlias: String?): String? {
        if (roomIdOrAlias == null) return null

        val isRoomId = try {
            RoomId(roomIdOrAlias); true
        } catch (e: IllegalStateException) {
            false
        }

        var alias: RoomAlias? = null
        if (!isRoomId) {
            alias = roomAliasService.findOne(roomIdOrAlias).toCompletionStage().await()
        }
        if (!isRoomId && alias == null) {
            return null
        }
        return if (isRoomId) roomIdOrAlias else alias!!.asJson().getString("room_id")

    }

}