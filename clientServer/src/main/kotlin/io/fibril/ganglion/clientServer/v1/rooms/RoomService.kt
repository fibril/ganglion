package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventDatabaseActions
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventUtils
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.CreateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.fibril.ganglion.clientServer.v1.rooms.dtos.CreateRoomDTO
import io.fibril.ganglion.clientServer.v1.rooms.models.Room
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAliasId
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException
import kotlinx.coroutines.future.await


interface RoomService : Service<Room> {
    suspend fun createRoomEventsBatch(createEventDTOs: List<CreateRoomEventDTO>): Future<List<RoomEvent>>
    suspend fun createRoomEvent(createRoomEventDTO: CreateRoomEventDTO): Future<RoomEvent?>
}

class RoomServiceImpl @Inject constructor(
    private val roomRepository: RoomRepository,
    private val roomEventService: RoomEventService,
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

        val eventsToCreate = generateEventsForRoomCreation(createRoomDTO)
        val createdRoomEvents = createRoomEventsBatch(eventsToCreate).toCompletionStage().await()

        if (createdRoomEvents.size == eventsToCreate.size) {
            // all events were created successfully
            // notify creation of room members that needs their avatar_url and display name updated
            val eventBus = vertx.eventBus()
            for (roomEvent in createdRoomEvents) {
                if (roomEvent.asJson().getString("type") == RoomEventNames.StateEvents.MEMBER) {
                    eventBus.send(RoomEventDatabaseActions.ROOM_MEMBER_CREATED, roomEvent.asJson())
                }
            }
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

    override suspend fun findOne(id: String): Future<Room> {
        TODO("Not yet implemented")
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

    // EVENTS

    override suspend fun createRoomEvent(createRoomEventDTO: CreateRoomEventDTO): Future<RoomEvent?> {
        val roomEvent = try {
            roomEventService.create(createRoomEventDTO).toCompletionStage().await()
        } catch (e: Exception) {
            return Future.failedFuture(e)
        }
        return Future.succeededFuture(roomEvent)
    }

    override suspend fun createRoomEventsBatch(createEventDTOs: List<CreateRoomEventDTO>): Future<List<RoomEvent>> {
        val roomEvents = mutableListOf<RoomEvent>()
        for (createEventDto in createEventDTOs) {
            try {
                val createdRoomEvent = createRoomEvent(createEventDto).toCompletionStage().await()
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

    @Throws(RequestException::class)
    private fun generateEventsForRoomCreation(createRoomDTO: CreateRoomDTO): List<CreateRoomEventDTO> {
        val senderId = createRoomDTO.sender?.principal()?.getString("sub")
        val createRoomJson = createRoomDTO.params()
        val roomId = createRoomJson.getString("id")
        val roomVersion = createRoomJson.getString("room_version")

        // A map to hold generated events that will follow when room is created
        val eventsMap: MutableMap<String, CreateRoomEventDTO> = mutableMapOf(
            // m.room.create
            RoomEventNames.StateEvents.CREATE to CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    put(
                        "content",
                        (createRoomJson.getJsonObject("creation_content") ?: JsonObject())
                            .mergeIn(
                                JsonObject.of("room_version", roomVersion ?: DEFAULT_ROOM_VERSION.toString())
                            )
                    )
                        .put("creator", senderId)
                        .put("sender", senderId)
                        .put("type", RoomEventNames.StateEvents.CREATE)
                        .put("state_key", RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY)
                        .put("room_id", roomId)
                },
                roomEventName = RoomEventNames.StateEvents.CREATE,
                sender = createRoomDTO.sender!!
            ),
            // m.room.member for creator/sender
            RoomEventNames.StateEvents.MEMBER to CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    put(
                        "content",
                        JsonObject.of(
                            "power_level", 100,
                            "avatar_url", null, // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "displayname", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "display_name", "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                            "is_direct", createRoomJson.getBoolean("is_direct"),
                            "membership", "join"

                        )
                    )
                        .put("sender", senderId)
                        .put("type", RoomEventNames.StateEvents.MEMBER)
                        .put("state_key", senderId)
                        .put("room_id", roomId)
                },
                roomEventName = RoomEventNames.StateEvents.MEMBER,
                sender = createRoomDTO.sender
            ),
            // m.room.power_levels
            RoomEventNames.StateEvents.POWER_LEVELS to CreateRoomEventDTO(
                json = RoomEventUtils.generateDefaultPowerLevelsEvent(
                    eventKeyValueOverrideJson = JsonObject()
                        .put("sender", senderId)
                        .put("creator", senderId)
                        .put("room_id", roomId),
                    contentKeyValueOverrideJson = JsonObject().apply {
                        if (createRoomJson.getJsonObject("power_level_content_override") != null) {
                            mergeIn(createRoomJson.getJsonObject("power_level_content_override"))
                        }
                    }
                ).asJson(),
                roomEventName = RoomEventNames.StateEvents.POWER_LEVELS,
                sender = createRoomDTO.sender
            )
        ).apply {
            if (createRoomJson.getJsonArray("initial_state") != null) {
                for (stateEvent in createRoomJson.getJsonArray("initial_state")) {
                    if ((stateEvent as JsonObject).getString("type") == RoomEventNames.StateEvents.CREATE) {
                        continue
                    }
                    put(
                        stateEvent.getString("type"), CreateRoomEventDTO(
                            json = JsonObject().mergeIn(stateEvent)
                                .apply { put("id", RoomEventUtils.generateEventId()) },
                            roomEventName = stateEvent.getString("type"),
                            sender = createRoomDTO.sender
                        )
                    )
                }
            }
        }


        // Apply preset if present
        val preset = createRoomJson.getString("preset")
        val defaultPresetJoinRuleEvent = CreateRoomEventDTO(
            json = JsonObject().apply {
                put("id", RoomEventUtils.generateEventId())
                put(
                    "content",
                    JsonObject.of("join_rule", "invite")
                )
                    .put("sender", senderId)
                    .put("type", RoomEventNames.StateEvents.JOIN_RULES)
                    .put("state_key", RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY)
                    .put("room_id", roomId)
            },
            roomEventName = RoomEventNames.StateEvents.JOIN_RULES,
            sender = createRoomDTO.sender
        )
        val defaultPresetHistoryVisibilityEvent = CreateRoomEventDTO(
            json = JsonObject().apply {
                put("id", RoomEventUtils.generateEventId())
                put(
                    "content",
                    JsonObject.of("history_visibility", "shared")
                )
                    .put("sender", senderId)
                    .put("type", RoomEventNames.StateEvents.HISTORY_VISIBILITY)
                    .put("state_key", RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY)
                    .put("room_id", roomId)
            },
            roomEventName = RoomEventNames.StateEvents.HISTORY_VISIBILITY,
            sender = createRoomDTO.sender
        )

        val defaultPresetGuestAccessEvent = CreateRoomEventDTO(
            json = JsonObject().apply {
                put("id", RoomEventUtils.generateEventId())
                put(
                    "content",
                    JsonObject.of("guest_access", "can_join")
                )
                    .put("sender", senderId)
                    .put("type", RoomEventNames.StateEvents.GUEST_ACCESS)
                    .put("state_key", senderId)
                    .put("room_id", roomId)
            },
            roomEventName = RoomEventNames.StateEvents.GUEST_ACCESS,
            sender = createRoomDTO.sender
        )

        when (preset) {
            "private_chat" -> {
                eventsMap[RoomEventNames.StateEvents.JOIN_RULES] = defaultPresetJoinRuleEvent
                eventsMap[RoomEventNames.StateEvents.HISTORY_VISIBILITY] = defaultPresetHistoryVisibilityEvent
                eventsMap[RoomEventNames.StateEvents.GUEST_ACCESS] = defaultPresetGuestAccessEvent
            }

            "trusted_private_chat" -> {
                eventsMap[RoomEventNames.StateEvents.JOIN_RULES] = defaultPresetJoinRuleEvent
                eventsMap[RoomEventNames.StateEvents.HISTORY_VISIBILITY] = defaultPresetHistoryVisibilityEvent
                eventsMap[RoomEventNames.StateEvents.GUEST_ACCESS] = defaultPresetGuestAccessEvent
            }

            "public_chat" -> {
                eventsMap[RoomEventNames.StateEvents.JOIN_RULES] =
                    CreateRoomEventDTO(
                        defaultPresetJoinRuleEvent.params().apply {
                            put(
                                "content",
                                JsonObject.of("join_rule", "public")
                            )
                        },
                        defaultPresetJoinRuleEvent.roomEventName,
                        defaultPresetJoinRuleEvent.sender
                    )
                eventsMap[RoomEventNames.StateEvents.HISTORY_VISIBILITY] = defaultPresetHistoryVisibilityEvent
                eventsMap[RoomEventNames.StateEvents.GUEST_ACCESS] =
                    CreateRoomEventDTO(
                        defaultPresetGuestAccessEvent.params().apply {
                            put(
                                "content",
                                JsonObject.of("guest_access", "forbidden")
                            )
                        },
                        defaultPresetGuestAccessEvent.roomEventName,
                        defaultPresetGuestAccessEvent.sender
                    )
            }
        }

        if (createRoomJson.getString("room_alias_name") != null) {
            eventsMap[RoomEventNames.StateEvents.CANONICAL_ALIAS] = CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    val alias = try {
                        RoomAliasId(
                            createRoomJson.getString("room_alias_name"),
                            ResourceBundleConstants.domain
                        ).toString()
                    } catch (e: IllegalStateException) {
                        throw RequestException(
                            statusCode = 500,
                            ErrorCodes.M_INVALID_PARAM.name,
                            StandardErrorResponse(
                                errCode = ErrorCodes.M_INVALID_PARAM,
                                error = e.message
                            ).asJson()
                        )

                    }
                    put(
                        "content",
                        JsonObject.of(
                            "alias", alias,
                            "alt_aliases", JsonArray.of(alias)
                        )
                    )
                        .put("sender", senderId)
                        .put("type", RoomEventNames.StateEvents.CANONICAL_ALIAS)
                        .put("state_key", RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY)
                        .put("room_id", roomId)
                },
                roomEventName = RoomEventNames.StateEvents.CANONICAL_ALIAS,
                sender = createRoomDTO.sender
            )
        }

        if (createRoomJson.getString("name") != null) {
            eventsMap[RoomEventNames.StateEvents.NAME] = CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    put(
                        "content",
                        JsonObject.of("name", createRoomJson.getString("name"))
                    )
                        .put("sender", senderId)
                        .put("type", RoomEventNames.StateEvents.NAME)
                        .put("state_key", RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY)
                        .put("room_id", roomId)
                },
                roomEventName = RoomEventNames.StateEvents.NAME,
                sender = createRoomDTO.sender
            )
        }

        if (createRoomJson.getString("topic") != null) {
            eventsMap[RoomEventNames.StateEvents.TOPIC] = CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    put(
                        "content",
                        JsonObject.of("topic", createRoomJson.getString("topic"))
                    )
                        .put("sender", senderId)
                        .put("type", RoomEventNames.StateEvents.TOPIC)
                        .put("state_key", RoomEventUtils.EVENT_ONE_OF_EACH_STATE_KEY)
                        .put("room_id", roomId)
                },
                roomEventName = RoomEventNames.StateEvents.TOPIC,
                sender = createRoomDTO.sender
            )
        }

        val eventsToCreate = mutableListOf<CreateRoomEventDTO>().apply {
            for (eventName in roomCreationEventNamesInOrder) {
                if (eventsMap[eventName] != null) {
                    add(eventsMap[eventName]!!)
                }
            }

            if (!(createRoomJson.getJsonArray("invite") ?: JsonArray()).isEmpty) {
                for (userId in createRoomJson.getJsonArray("invite").toList()) {
                    add(
                        CreateRoomEventDTO(
                            json = JsonObject().apply {
                                put("id", RoomEventUtils.generateEventId())
                                put(
                                    "content",
                                    JsonObject.of(
                                        "power_level",
                                        if (createRoomJson.getString("preset") == "trusted_private_chat") 100 else 0,
                                        "avatar_url",
                                        null, // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                                        "displayname",
                                        "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                                        "display_name",
                                        "", // TO BE POPULATED IN A BACKGROUND JOB IN A WORKER VERTICLE
                                        "is_direct",
                                        createRoomJson.getBoolean("is_direct"),
                                        "membership",
                                        "invite"
                                    )
                                )
                                    .put("sender", senderId)
                                    .put("type", RoomEventNames.StateEvents.MEMBER)
                                    .put("state_key", userId)
                                    .put("room_id", roomId)
                            },
                            roomEventName = RoomEventNames.StateEvents.MEMBER,
                            sender = createRoomDTO.sender
                        )
                    )
                }
            }
            if ((createRoomJson.getJsonArray("invite_3pid")?.size() ?: 0) > 0) {
                // TODO:- Do 3rd Party Invite
            }
        }

        return eventsToCreate

    }

}