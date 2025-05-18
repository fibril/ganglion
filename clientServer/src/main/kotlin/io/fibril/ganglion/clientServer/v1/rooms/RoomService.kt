package io.fibril.ganglion.clientServer.v1.rooms

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.exclude
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.utils.pagination.KeySetPagination
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.filters.FilterService
import io.fibril.ganglion.clientServer.v1.filters.models.EventFilter
import io.fibril.ganglion.clientServer.v1.filters.models.RoomEventFilter
import io.fibril.ganglion.clientServer.v1.filters.models.RoomFilter
import io.fibril.ganglion.clientServer.v1.presence.PresenceServiceImpl
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventUtils
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomMembershipState
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.CreateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.UpdateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.fibril.ganglion.clientServer.v1.rooms.dtos.*
import io.fibril.ganglion.clientServer.v1.rooms.models.Room
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAlias
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.clientServer.v1.typing.TypingService
import io.fibril.ganglion.storage.impl.GanglionOpenSearch
import io.fibril.ganglion.storage.impl.GanglionRedisClient
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.pgclient.PgException
import io.vertx.redis.client.RedisAPI
import io.vertx.sqlclient.DatabaseException
import kotlinx.coroutines.future.await
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import io.vertx.ext.auth.User as VertxUser


interface RoomService : Service<Room> {
    suspend fun sync(syncDTO: SyncDTO): Future<JsonObject>

    suspend fun getRoomMembershipEventsForUser(
        userId: String,
        roomMembershipState: RoomMembershipState
    ): Future<List<RoomEvent>>

    suspend fun getRoomMembershipEventsForUser(
        user: VertxUser,
        roomMembershipState: RoomMembershipState
    ): Future<List<RoomEvent>>

    suspend fun inviteUser(inviteUserDTO: InviteUserDTO): Future<RoomEvent>
    suspend fun joinViaRoomIdOrAlias(joinRoomViaRoomIdOrAliasDTO: JoinRoomViaRoomIdOrAliasDTO): Future<RoomEvent>
    suspend fun joinViaRoomId(joinRoomViaRoomIdDTO: JoinRoomViaRoomIdDTO): Future<RoomEvent>
    suspend fun knockRoom(knockRoomDTO: KnockRoomDTO): Future<RoomEvent>
    suspend fun leaveRoom(leaveRoomDTO: LeaveRoomDTO): Future<RoomEvent>
    suspend fun forgetRoom(forgetRoomDTO: ForgetRoomDTO): Future<RoomEvent>
    suspend fun kickUser(kickUserDTO: KickUserDTO): Future<RoomEvent>
    suspend fun banUser(banUserDTO: BanUserDTO): Future<RoomEvent>
    suspend fun unBanUser(unBanUserDTO: UnBanUserDTO): Future<RoomEvent>


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
    private val filterService: FilterService,
    private val typingService: TypingService,
    private val ganglionOpenSearch: GanglionOpenSearch,
    private val ganglionRedisClient: GanglionRedisClient,

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
            RoomEventNames.StateEvents.GUEST_ACCESS
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

    override suspend fun getRoomMembershipEventsForUser(
        userId: String,
        roomMembershipState: RoomMembershipState
    ): Future<List<RoomEvent>> {
        val roomEvents = try {
            roomEventService.fetchEvents(
                mapOf(
                    "state_key =" to userId,
                    "type =" to RoomEventNames.StateEvents.MEMBER,
                    "content @>" to "{${'"'}${"membership"}${'"'}: ${'"'}${RoomMembershipState.JOIN.name.lowercase()}${'"'}}"
                )
            ).toCompletionStage().await()
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }

        return Future.succeededFuture(roomEvents ?: listOf())
    }

    override suspend fun getRoomMembershipEventsForUser(
        user: VertxUser,
        roomMembershipState: RoomMembershipState
    ): Future<List<RoomEvent>> {
        return getRoomMembershipEventsForUser(user.principal().getString("sub"), roomMembershipState)
    }

    override suspend fun findOne(id: String): Future<Room?> {
        val room = try {
            roomRepository.find(id)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        return Future.succeededFuture(room)
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<Room>> {
        val keySetPagination = KeySetPagination(paginationDTO)
        val results = try {
            roomRepository.findAll(keySetPagination.prepareQuery(RoomRepositoryImpl.LIST_PUBLIC_ROOMS_QUERY))
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        val paginatedResult = keySetPagination.usingQueryResult<Room>(results).paginatedResult
        return Future.succeededFuture(paginatedResult)
    }

    override suspend fun update(id: String, dto: DTO): Future<Room> {
        val room = try {
            roomRepository.update(id, dto)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Exception",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        } ?: return Future.failedFuture(
            RequestException(
                404,
                "Room not found",
                StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson()
            )
        )

        return Future.succeededFuture(room)
    }

    override suspend fun remove(id: String): Future<Room> {
        val deletedRoom = try {
            roomRepository.delete(id)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        if (deletedRoom != null) return Future.succeededFuture(deletedRoom)
        return Future.failedFuture(
            RequestException(
                500,
                "Unknown Error",
                StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
            )
        )

    }


    override suspend fun sync(syncDTO: SyncDTO): Future<JsonObject> {
        val vertxUser = syncDTO.sender ?: return Future.failedFuture(
            RequestException(
                403,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )

        val currentUserId = vertxUser.principal().getString("sub")

        val params = syncDTO.params()
        val filterParam = params.getValue("filter", null)?.toString()
        val filter: JsonObject =
            if (filterParam == null) JsonObject()
            else if (filterParam.startsWith('{')) JsonObject(filterParam)
            else filterService.findOne(filterParam)
                .toCompletionStage().await()?.asJson() ?: JsonObject()

        val initialFilter = RoomEventFilter(filter)


        val userJoinedRoomEvents =
            getRoomMembershipEventsForUser(currentUserId, RoomMembershipState.JOIN).toCompletionStage().await()

        /** All the roomIds that the user has joined */
        val joinedRoomIds = userJoinedRoomEvents.mapNotNull { it.asJson().getString("room_id") }

        /** Permitted types of events in Restricted rooms */
        val strippedStateTypes = listOf(
            RoomEventNames.StateEvents.CREATE,
            RoomEventNames.StateEvents.NAME,
            RoomEventNames.StateEvents.AVATAR,
            RoomEventNames.StateEvents.TOPIC,
            RoomEventNames.StateEvents.JOIN_RULES,
            RoomEventNames.StateEvents.CANONICAL_ALIAS,
            RoomEventNames.StateEvents.ENCRYPTION
        )

        val joinedRoomsQuery = """
                SELECT * FROM room_events 
                WHERE type = '${RoomEventNames.StateEvents.MEMBER}' 
                AND content @> '{${'"'}${"membership"}${'"'}: ${'"'}${RoomMembershipState.JOIN.name.lowercase()}${'"'}}' 
                AND room_id = ANY(ARRAY[${joinedRoomIds.map { "${"'"}$it${"'"}" }.joinToString(", ")}]);
            """.trimIndent()

        val joinedRoomStateEvents = roomEventService.fetchEventsByQuery(
            joinedRoomsQuery
        ).toCompletionStage().await()

        println(joinedRoomsQuery)

        println("joinedRoomStateEvents ${joinedRoomStateEvents?.map { it.asJson() }}")

        val usersSenderSharesRoomWith =
            joinedRoomStateEvents?.mapNotNull { it.asJson().getString("state_key") } ?: listOf()

        val accountDataEventNames = listOf(RoomEventNames.AccountDataEvents.TAG)


        val accountDataEventFilter = EventFilter(filter?.getJsonObject("account_data"))
        val presenceEventFilter = EventFilter(filter?.getJsonObject("presence", JsonObject()))
        val roomFilter = RoomFilter(filter?.getJsonObject("room", JsonObject()))

        val accountDataQuery = accountDataEventFilter.toQuery(
            additionalShouldQueries = listOf(
//                Has no room_id (global account data)
                Query.of { noRoomQuery ->
                    noRoomQuery.bool { nb ->
                        nb.mustNot { mn ->
                            mn.exists { e -> e.field("map.room_id") }
                        }.must { m ->
                            m.term { t -> t.field("map.sender").value(FieldValue.of(currentUserId)) }
                        }
                    }
                }
            )
        )


        val inviteFilter = RoomEventFilter(filter)
        val userInviteQuery = inviteFilter.toQuery(
            lazyLoadMembersForUserId = if (initialFilter.lazyLoadMembers) currentUserId else null,
            additionalShouldQueries = listOf(
                Query.of { qq ->
                    qq.term { t ->
                        t.field("map.sender.keyword")
                            .value(FieldValue.of(currentUserId))
                    }
                },
                Query.of { qq ->
                    qq.term { t ->
                        t.field("map.content.membership.keyword")
                            .value(FieldValue.of(RoomMembershipState.INVITE.name.lowercase()))
                    }
                },
            )
        )

        val knockFilter = RoomEventFilter(filter)
        val userKnockQuery = knockFilter.toQuery(
            lazyLoadMembersForUserId = if (initialFilter.lazyLoadMembers) currentUserId else null,
            additionalShouldQueries = listOf(
                Query.of { qq ->
                    qq.term { t ->
                        t.field("map.sender.keyword")
                            .value(FieldValue.of(currentUserId))
                    }
                },
                Query.of { qq ->
                    qq.term { t ->
                        t.field("map.content.membership.keyword")
                            .value(FieldValue.of(RoomMembershipState.KNOCK.name.lowercase()))
                    }
                },
            )
        )

        val userLeaveQuery =
            syncBuildRoomMembershipQuery(listOf(), currentUserId, RoomMembershipState.LEAVE)

        val joinedRoomFilter = RoomEventFilter(filter.copy().apply {
            put("rooms", JsonArray.of(*joinedRoomIds.toTypedArray()))
        })

        val joinedRoomsAccountDataQuery = joinedRoomFilter.toQuery(
            lazyLoadMembersForUserId = if (initialFilter.lazyLoadMembers) currentUserId else null,
            additionalMustQueries = listOf(
                Query.of { noRoomQuery ->
                    noRoomQuery.bool { nb ->
                        nb.must { mn ->
                            mn.exists { e -> e.field("map.room_id") }
                        }.must { m ->
                            m.term { t -> t.field("map.sender.keyword").value(FieldValue.of(currentUserId)) }
                        }
                    }
                }
            )
        )

        val joinedRoomsTimelineQuery = joinedRoomFilter.toQuery(
            lazyLoadMembersForUserId = if (initialFilter.lazyLoadMembers) currentUserId else null,
        )

        val joinedRoomsStateQuery = joinedRoomFilter.toQuery(
            lazyLoadMembersForUserId = if (initialFilter.lazyLoadMembers) currentUserId else null,
            additionalMustQueries = listOf(
                Query.of { q ->
                    q.bool { nb ->
                        nb.must { m ->
                            m.terms { t ->
                                t.field("map.content.membership.keyword")
                                    .terms { v -> v.value(strippedStateTypes.map { FieldValue.of(it) }) }
                            }
                        }
                    }
                }
            )
        )


        println("joinedRoomsTimelineQuery ${joinedRoomsTimelineQuery}")

        println("usersSenderSharesRoomWith $usersSenderSharesRoomWith")

        var presenceEvents = mutableListOf<JsonObject>()
        if (usersSenderSharesRoomWith.isNotEmpty()) {
            val redisClient = ganglionRedisClient.client()
            val redisAPI = RedisAPI.api(redisClient)
            val resp =
                redisAPI.mget(usersSenderSharesRoomWith.map { PresenceServiceImpl.makeKey(it) }).toCompletionStage()
                    .await()

            if (resp.isArray) {
                presenceEvents = resp.toList().mapNotNull { it }.map { JsonObject(it.toString()) }.toMutableList()
            } else if (resp.isMap) {
                presenceEvents.add(JsonObject(resp.toString()))
            }
        }


        println(presenceEvents.map { it })

        val joinTypingEvents = joinedRoomIds.mapNotNull {
            try {
                typingService.getTypingUsers(it).toCompletionStage().await()
            } catch (pgE: Exception) {
                null
            }
        }

        println(joinTypingEvents.map { it.asJson() })

        val openSearchClient = ganglionOpenSearch.client()

        val accountDataRequest = SearchRequest.Builder()
            .index("room_events")
            .query(accountDataQuery)
            .size(10)
            .build()

        val userInviteRequest = SearchRequest.Builder()
            .index("room_events")
            .query(userInviteQuery)
            .size(10)
            .build()

        val userKnockRequest = SearchRequest.Builder()
            .index("room_events")
            .query(userKnockQuery)
            .size(10)
            .build()

        val userLeaveRequest = SearchRequest.Builder()
            .index("room_events")
            .query(userLeaveQuery)
            .size(10)
            .build()


        val accountDataEvents = openSearchClient.search(accountDataRequest, JsonObject::class.java)
        val userInviteEvents = openSearchClient.search(userInviteRequest, JsonObject::class.java)
        val userKnockEvents = openSearchClient.search(userKnockRequest, JsonObject::class.java)
        val userLeaveEvents = openSearchClient.search(userLeaveRequest, JsonObject::class.java)

        val joinedRoomAccountDataRequest = SearchRequest.Builder()
            .index("room_events")
            .query(joinedRoomsAccountDataQuery)
            .size(10)
            .build()

        val joinedRoomStateRequest = SearchRequest.Builder()
            .index("room_events")
            .query(joinedRoomsStateQuery)
            .size(10)
            .build()

        val joinedRoomTimelineRequest = SearchRequest.Builder()
            .index("room_events")
            .query(joinedRoomsTimelineQuery)
            .size(10)
            .build()

        val joinedRoomAccountDataEventsResponse =
            openSearchClient.search(joinedRoomTimelineRequest, JsonObject::class.java)

        val joinedRoomStateEventsResponse = openSearchClient.search(joinedRoomTimelineRequest, JsonObject::class.java)

        val joinedRoomTimeLineEventsResponse =
            openSearchClient.search(joinedRoomTimelineRequest, JsonObject::class.java)

//        val hits = joinedRoomTimeLineEvents.hits().hits()
//        println(hits.size)
//        for (hit in hits) {
//            println(hit.source())
//        }

        val joinedRoomAccountMap = joinedRoomAccountDataEventsResponse.hits().hits()
            .mapNotNull { it.source() }
            .groupBy { it.getString("room_id") }

        val joinedRoomEphemeralMap = joinedRoomTimeLineEventsResponse.hits().hits()
            .mapNotNull { it.source() }
            .groupBy { it.getString("room_id") }

        val joinedRoomStateEventsMap = joinedRoomStateEventsResponse.hits().hits()
            .mapNotNull { it.source() }
            .groupBy { it.getString("room_id") }

        val joinTimelineMap = joinedRoomTimeLineEventsResponse.hits().hits()
            .mapNotNull { it.source() }
            .groupBy { it.getString("room_id") }


        val result = JsonObject()
            .put(
                "account_data",
                JsonObject.of(
                    "events",
                    accountDataEvents.hits().hits().mapNotNull { it.source() })
            )
            .put("presence", JsonObject.of("events", presenceEvents))
            .put(
                "rooms",
                JsonObject()
                    .put(
                        RoomMembershipState.INVITE.name.lowercase(),
                        JsonObject(
                            userInviteEvents.hits().hits()
                                .mapNotNull { it.source() }
                                .groupBy { it.getString("room_id") }
                        )
                    )
                    .put(
                        RoomMembershipState.KNOCK.name.lowercase(),
                        JsonObject(
                            userKnockEvents.hits().hits()
                                .mapNotNull { it.source() }
                                .groupBy { it.getString("room_id") }
                        )
                    )
                    .put(
                        RoomMembershipState.LEAVE.name.lowercase(),
                        JsonObject(
                            userLeaveEvents.hits().hits()
                                .mapNotNull { it.source() }
                                .groupBy { it.getString("room_id") }
                        )
                    )
                    .put(
                        RoomMembershipState.JOIN.name.lowercase(),
                        JsonObject().apply {
                            for (roomId in joinedRoomIds) {
                                put(
                                    roomId,
                                    JsonObject()
                                        .put(
                                            "account_data", JsonObject.of(
                                                "events",
                                                accountDataEvents.hits().hits().mapNotNull { it.source() })
                                        )
                                        .put(
                                            "ephemeral", JsonObject.of(
                                                "events",
                                                joinedRoomEphemeralMap.getOrDefault(roomId, JsonArray.of())
                                            )
                                        )
                                        .put(
                                            "state", JsonObject.of(
                                                "events",
                                                joinedRoomStateEventsMap.getOrDefault(roomId, JsonArray.of())
                                            )
                                        )
                                        .put(
                                            "summary", JsonObject.of(
                                                "events",
                                                JsonObject()
                                            )
                                        )
                                        .put(
                                            "timeline", JsonObject.of(
                                                "events",
                                                joinTimelineMap.getOrDefault(roomId, JsonArray.of())
                                            )
                                        )

                                        .put(
                                            "unread_notifications", JsonObject.of(
                                                "events",
                                                joinTimelineMap.getOrDefault(roomId, JsonArray.of())
                                            )
                                        )

                                        .put(
                                            "unread_thread_notifications", JsonObject.of(
                                                "events",
                                                joinTimelineMap.getOrDefault(roomId, JsonArray.of())
                                            )
                                        )
                                )
                            }
                        }
                    )
            )

        return Future.succeededFuture(result)

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
        return Future.failedFuture(
            RequestException(
                403,
                "Member invite denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Member invite denied").asJson()
            )
        )
    }

    override suspend fun joinViaRoomIdOrAlias(joinRoomViaRoomIdOrAliasDTO: JoinRoomViaRoomIdOrAliasDTO): Future<RoomEvent> {
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

    override suspend fun joinViaRoomId(joinRoomViaRoomIdDTO: JoinRoomViaRoomIdDTO): Future<RoomEvent> {
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

    override suspend fun knockRoom(knockRoomDTO: KnockRoomDTO): Future<RoomEvent> {
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

    override suspend fun leaveRoom(leaveRoomDTO: LeaveRoomDTO): Future<RoomEvent> {
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

        return roomEventService.update(leaverRoomEvent.id, updateRoomEventDTO)
    }

    override suspend fun forgetRoom(forgetRoomDTO: ForgetRoomDTO): Future<RoomEvent> {
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

        return roomEventService.remove(leaverRoomEvent.id)
    }

    override suspend fun kickUser(kickUserDTO: KickUserDTO): Future<RoomEvent> {
        val params = kickUserDTO.params()
        val senderId = kickUserDTO.sender?.principal()?.getString("sub") ?: ""
        val roomId = params.getString("roomId")
        val userId = params.getString("user_id")
        val canKickUser = canKick(roomId, senderId, userId).toCompletionStage().await()
        if (canKickUser) {
            return kick(roomId, userId)
        }
        return Future.failedFuture(
            RequestException(
                403,
                "Member kick denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Member kick denied").asJson()
            )
        )
    }

    override suspend fun banUser(banUserDTO: BanUserDTO): Future<RoomEvent> {
        val params = banUserDTO.params()
        val senderId = banUserDTO.sender?.principal()?.getString("sub") ?: ""
        val roomId = params.getString("roomId")
        val userId = params.getString("user_id")
        val canBanUser = canBan(roomId, senderId, userId).toCompletionStage().await()
        if (canBanUser) {
            return ban(roomId, userId)
        }
        return Future.failedFuture(
            RequestException(
                403,
                "Member ban denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Member ban denied").asJson()
            )
        )
    }

    override suspend fun unBanUser(unBanUserDTO: UnBanUserDTO): Future<RoomEvent> {
        val params = unBanUserDTO.params()
        val senderId = unBanUserDTO.sender?.principal()?.getString("sub") ?: ""
        val roomId = params.getString("roomId")
        val userId = params.getString("user_id")
        val canUnBanUser = canUnBan(roomId, senderId, userId).toCompletionStage().await()
        if (canUnBanUser) {
            return unBan(roomId, userId)
        }
        return Future.failedFuture(
            RequestException(
                403,
                "Member unban denied",
                StandardErrorResponse(ErrorCodes.M_UNAUTHORIZED, "Member unban denied").asJson()
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
                        "state_key =" to userId,
                        "type =" to RoomEventNames.StateEvents.MEMBER
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

    suspend fun canUnBan(roomId: String, roomMemberId: String, anotherMemberId: String): Future<Boolean> {
        // if can ban a user, then you can unban same user
        return canBan(roomId, roomMemberId, anotherMemberId)
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
                        "state_key =" to userId,
                        "type =" to RoomEventNames.StateEvents.MEMBER
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

    private suspend fun joinRoom(roomId: String, vertxUser: User): Future<RoomEvent> {
        val joinerId = vertxUser.principal()?.getString("sub") ?: return Future.failedFuture(
            RequestException(
                401,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )
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

            return roomEventService.create(createRoomEventDTO)
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
            return roomEventService.update(inviteeRoomEvent.id, updateRoomEventDTO)
        }
    }

    private suspend fun knock(roomId: String, vertxUser: User): Future<RoomEvent> {
        val knockerId = vertxUser.principal()?.getString("sub") ?: return Future.failedFuture(
            RequestException(
                401,
                "Unauthorized",
                StandardErrorResponse(ErrorCodes.M_BAD_JSON, "No user found").asJson()
            )
        )
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

            return roomEventService.create(createRoomEventDTO)
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

            return roomEventService.update(knockerRoomEvent.id, updateRoomEventDTO)
        }
        return Future.failedFuture(
            RequestException(
                500,
                "Unknown Error",
                StandardErrorResponse(ErrorCodes.M_UNKNOWN, "Cannot knock on room").asJson()
            )
        )
    }

    private suspend fun kick(roomId: String, userToKickId: String): Future<RoomEvent> {
        val userToKickRoomEvent =
            roomEventService.getRoomMemberEvent(roomId, userToKickId) ?: return Future.failedFuture(
                RequestException(
                    403,
                    "",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN, "User not in room").asJson()
                )
            )
        val powerLevelEvent = roomEventService.getRoomPowerLevelEvent(roomId)
        val defaultPowerLevelForLeavers = JsonObject(
            powerLevelEvent?.asJson()?.getString("content") ?: JsonObject().toString()
        ).getString("users_default") ?: 0

        val updateRoomEventDTO = UpdateRoomEventDTO(
            json = JsonObject().apply {
                put(
                    "content",
                    JsonObject.of(
                        "membership", RoomMembershipState.LEAVE.name.lowercase(),
                        "power_level", defaultPowerLevelForLeavers
                    ).mergeIn(
                        JsonObject(
                            userToKickRoomEvent.asJson().getString("content")
                        ).exclude("membership", "power_level")
                    )
                )
            },
            roomEventName = RoomEventNames.StateEvents.MEMBER,
            sender = null
        )

        return roomEventService.update(userToKickRoomEvent.id, updateRoomEventDTO)
    }

    private suspend fun ban(roomId: String, userToBanId: String): Future<RoomEvent> {
        val userToBanRoomEvent =
            roomEventService.getRoomMemberEvent(roomId, userToBanId) ?: return Future.failedFuture(
                RequestException(
                    403,
                    "",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN, "User not in room").asJson()
                )
            )

        val updateRoomEventDTO = UpdateRoomEventDTO(
            json = JsonObject().apply {
                put(
                    "content",
                    JsonObject.of(
                        "membership", RoomMembershipState.BAN.name.lowercase(),
                        "power_level", -1
                    ).mergeIn(
                        JsonObject(
                            userToBanRoomEvent.asJson().getString("content")
                        ).exclude("membership", "power_level")
                    )
                )
            },
            roomEventName = RoomEventNames.StateEvents.MEMBER,
            sender = null
        )

        return roomEventService.update(userToBanRoomEvent.id, updateRoomEventDTO)
    }

    private suspend fun unBan(roomId: String, userToUnBanId: String): Future<RoomEvent> {
        val userToBanRoomEvent =
            roomEventService.getRoomMemberEvent(roomId, userToUnBanId) ?: return Future.failedFuture(
                RequestException(
                    403,
                    "",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN, "User not in room").asJson()
                )
            )
        val powerLevelEvent = roomEventService.getRoomPowerLevelEvent(roomId)
        val defaultPowerLevelForLeavers = JsonObject(
            powerLevelEvent?.asJson()?.getString("content") ?: JsonObject().toString()
        ).getString("users_default") ?: 0

        val updateRoomEventDTO = UpdateRoomEventDTO(
            json = JsonObject().apply {
                put(
                    "content",
                    JsonObject.of(
                        "membership", RoomMembershipState.LEAVE.name.lowercase(),
                        "power_level", defaultPowerLevelForLeavers
                    ).mergeIn(
                        JsonObject(
                            userToBanRoomEvent.asJson().getString("content")
                        ).exclude("membership", "power_level")
                    )
                )
            },
            roomEventName = RoomEventNames.StateEvents.MEMBER,
            sender = null
        )

        return roomEventService.update(userToBanRoomEvent.id, updateRoomEventDTO)
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
                    // room member must be in the room
                    roomMemberEvent != null &&
                    JsonObject(roomMemberEvent.asJson().getString("content"))
                        .getString("membership") == RoomMembershipState.JOIN.name &&
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


    private fun syncBuildRoomMembershipQuery(
        types: List<String>,
        currentUserId: String,
        roomMembershipState: RoomMembershipState
    ): Query {
        return Query.of { q ->
            q.bool { b ->
                b.should(
                    Query.of { mustQuery ->
                        mustQuery.bool { bb ->
                            bb.must(
                                Query.of { typeQuery ->
                                    typeQuery.match { t ->
                                        t.field("map.state_key")
                                            .query { v -> v.stringValue(currentUserId) }
                                    }
                                },
                                Query.of { typeQuery ->
                                    typeQuery.match { t ->
                                        t.field("map.type")
                                            .query { v -> v.stringValue(RoomEventNames.StateEvents.MEMBER) }
                                    }
                                },
                                Query.of { typeQuery ->
                                    typeQuery.match { t ->
                                        t.field("map.content.membership")
                                            .query { v -> v.stringValue(roomMembershipState.name.lowercase()) }
                                    }
                                }
                            )
                        }
                    },
                    *types.map { type ->
                        Query.of { qq ->
                            qq.match { m ->
                                m.field("map.type")
                                    .query(FieldValue.of(type))
                            }
                        }
                    }.toTypedArray()
                ).minimumShouldMatch("2")
            }
        }
    }

}