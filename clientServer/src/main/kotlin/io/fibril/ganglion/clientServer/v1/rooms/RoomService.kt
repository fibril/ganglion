package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventRepository
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventUtils
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.CreateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.fibril.ganglion.clientServer.v1.rooms.dtos.CreateRoomAliasDTO
import io.fibril.ganglion.clientServer.v1.rooms.dtos.CreateRoomDTO
import io.fibril.ganglion.clientServer.v1.rooms.models.Room
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAlias
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAliasId
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException
import kotlinx.coroutines.future.await


interface RoomService : Service<Room> {
    suspend fun createRoomAlias(createRoomAliasDTO: CreateRoomAliasDTO): Future<RoomAlias?>
    suspend fun createRoomEventsBatch(createEventDTOs: List<CreateRoomEventDTO>): Future<List<RoomEvent>>
    suspend fun createRoomEvent(createRoomEventDTO: CreateRoomEventDTO): Future<RoomEvent?>
}

class RoomServiceImpl @Inject constructor(
    private val roomRepository: RoomRepository,
    private val roomEventRepository: RoomEventRepository
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
        val createRoomJson = (dto as CreateRoomDTO).params().apply {
            if (getString("id") == null) {
                put("id", Utils.generateRoomId())
            }
        }
        println("createRoomJson $createRoomJson")
        val senderId = dto.sender?.principal()?.getString("sub")

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
                sender = dto.sender!!
            ),
            // m.room.member for creator/sender
            RoomEventNames.StateEvents.MEMBER to CreateRoomEventDTO(
                json = JsonObject().apply {
                    put("id", RoomEventUtils.generateEventId())
                    put(
                        "content",
                        JsonObject.of(
                            "avatar_url", null,
                            "displayname", "",
                            "display_name", "",
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
                sender = dto.sender!!
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
                        if (createRoomJson.getString("preset") == "trusted_private_chat" && (createRoomJson.getJsonArray(
                                "invite"
                            )
                                ?.size() ?: 0) > 0
                        ) {
                            val users = JsonObject().apply {
                                createRoomJson.getJsonArray("invite").toList().forEach {
                                    put(it as String, 100)
                                }
                            }
                            put("users", users)
                        }
                    }
                ).asJson(),
                roomEventName = RoomEventNames.StateEvents.POWER_LEVELS,
                sender = dto.sender!!
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
                            sender = dto.sender!!
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
            sender = dto.sender!!
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
            sender = dto.sender!!
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
            sender = dto.sender!!
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
                        return Future.failedFuture(
                            RequestException(
                                statusCode = 500,
                                ErrorCodes.M_INVALID_PARAM.name,
                                StandardErrorResponse(
                                    errCode = ErrorCodes.M_INVALID_PARAM,
                                    error = e.message
                                ).asJson()
                            )
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
                sender = dto.sender!!
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
                sender = dto.sender!!
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
                sender = dto.sender!!
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
                                        "avatar_url", null,
                                        "displayname", "",
                                        "display_name", "",
                                        "is_direct", createRoomJson.getBoolean("is_direct"),
                                        "membership", "invite"
                                    )
                                )
                                    .put("sender", senderId)
                                    .put("type", RoomEventNames.StateEvents.MEMBER)
                                    .put("state_key", userId)
                                    .put("room_id", roomId)
                            },
                            roomEventName = RoomEventNames.StateEvents.MEMBER,
                            sender = dto.sender!!
                        )
                    )
                }
            }
            if ((createRoomJson.getJsonArray("invite_3pid")?.size() ?: 0) > 0) {
                // TODO:- Do 3rd Party Invite
            }
        }

        val createdRoomEvents = createRoomEventsBatch(eventsToCreate).toCompletionStage().await()

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

    override suspend fun createRoomAlias(createRoomAliasDTO: CreateRoomAliasDTO): Future<RoomAlias?> {
        val roomAlias = roomRepository.createRoomAlias(createRoomAliasDTO)
        return Future.succeededFuture(roomAlias)
    }


    // EVENTS

    override suspend fun createRoomEvent(createRoomEventDTO: CreateRoomEventDTO): Future<RoomEvent?> {
        val roomEvent = try {
            roomEventRepository.save(createRoomEventDTO)
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

}