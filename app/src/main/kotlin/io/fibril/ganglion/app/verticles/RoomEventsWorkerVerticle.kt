package io.fibril.ganglion.app.verticles

import com.google.inject.Guice
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.extensions.only
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventDatabaseActions
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventRepository
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.UpdateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.fibril.ganglion.clientServer.v1.rooms.RoomAliasRepository
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAlias
import io.fibril.ganglion.clientServer.v1.users.UserProfileRepository
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.ThreadingModel
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

class RoomEventsWorkerVerticle : CoroutineVerticle() {
    private lateinit var roomAliasRepository: RoomAliasRepository
    private lateinit var roomEventRepository: RoomEventRepository
    private lateinit var userProfileRepository: UserProfileRepository

    override suspend fun start() {
        val injector = Guice.createInjector(ClientModule(vertx))
        roomAliasRepository = injector.getInstance(RoomAliasRepository::class.java)
        roomEventRepository = injector.getInstance(RoomEventRepository::class.java)
        userProfileRepository = injector.getInstance(UserProfileRepository::class.java)

        val eventBus = vertx.eventBus()

        eventBus.consumer(RoomEventDatabaseActions.ROOM_ALIAS_CREATED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                roomAliasCreated(message)
                    .onSuccess {
                        println("roomAliasCreated method from Verticle")
                    }
                    .onFailure { e ->
                        println("roomAliasCreated Failed from Verticle ${e.message}")
                    }
            }

        }
        eventBus.consumer(RoomEventDatabaseActions.ROOM_ALIAS_UPDATED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                roomAliasUpdated(message)
                    .onSuccess {
                        println("roomAliasUpdated method from Verticle")
                    }
                    .onFailure { e ->
                        println("roomAliasUpdated Failed from Verticle ${e.message}")
                    }
            }
        }
        eventBus.consumer(RoomEventDatabaseActions.ROOM_ALIAS_DELETED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                roomAliasDeleted(message)
                    .onSuccess {
                        println("roomAliasDeleted method from Verticle")
                    }
                    .onFailure { e ->
                        println("roomAliasDeleted Failed from Verticle ${e.message}")
                    }
            }
        }


        eventBus.consumer(RoomEventDatabaseActions.ROOM_MEMBER_CREATED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                roomMemberCreated(message)
                    .onSuccess {
                        println("roomMemberCreated method from Verticle")
                    }
                    .onFailure { e ->
                        println("roomMemberCreated Failed from Verticle ${e.message}")
                    }
            }
        }
    }

    private suspend fun roomAliasCreated(message: Message<JsonObject>): Future<Void> {
        val createdRoomAlias = RoomAlias(message.body())
        val roomCanonicalAliasEvent =
            getCanonicalAliasEvent(createdRoomAlias.asJson().getString("room_id")) ?: return Future.succeededFuture()

        val roomCanonicalAliasEventContent = JsonObject(roomCanonicalAliasEvent.asJson().getString("content"))


        val altAliases = roomCanonicalAliasEventContent.getJsonArray("alt_aliases") ?: JsonArray()

        val canonicalAliasEventContent = JsonObject.of(
            "content",
            roomCanonicalAliasEventContent.copy().mergeIn(
                JsonObject.of(
                    "alt_aliases", JsonArray.of(
                        *altAliases.toMutableSet().apply { add(createdRoomAlias.id) }.toTypedArray()
                    )
                )
            )
        )
        return updateCanonicalAliasContent(roomCanonicalAliasEvent.id, canonicalAliasEventContent)
    }

    private suspend fun roomAliasUpdated(message: Message<JsonObject>): Future<Void> {
        val updatedRoomAlias = RoomAlias(message.body())
        val roomCanonicalAliasEvent =
            getCanonicalAliasEvent(updatedRoomAlias.asJson().getString("room_id")) ?: return Future.succeededFuture()

        val roomCanonicalAliasEventContent = JsonObject(roomCanonicalAliasEvent.asJson().getString("content"))

        val altAliases = roomCanonicalAliasEventContent.getJsonArray("alt_aliases") ?: JsonArray()

        if (altAliases.contains(updatedRoomAlias.id)) {
            return Future.succeededFuture()
        }


        val canonicalAliasEventContent = JsonObject.of(
            "content",
            roomCanonicalAliasEventContent.copy().put(
                "alt_aliases", JsonArray.of(
                    *altAliases.toMutableSet().apply { add(updatedRoomAlias.id) }.toTypedArray()
                )
            )
        )
        return updateCanonicalAliasContent(roomCanonicalAliasEvent.id, canonicalAliasEventContent)
    }

    private suspend fun roomAliasDeleted(message: Message<JsonObject>): Future<Void> {
        val deletedRoomAlias = RoomAlias(message.body())
        val roomCanonicalAliasEvent =
            getCanonicalAliasEvent(deletedRoomAlias.asJson().getString("room_id")) ?: return Future.succeededFuture()

        val roomCanonicalAliasEventContent = JsonObject(roomCanonicalAliasEvent.asJson().getString("content"))

        val alias = roomCanonicalAliasEventContent.getString("alias")

        if (alias != null && alias == deletedRoomAlias.id) {
            try {
                roomEventRepository.delete(
                    roomCanonicalAliasEvent.id
                )
                return Future.succeededFuture()
            } catch (e: Exception) {
                return Future.failedFuture(e)
            }
        }

        val altAliases = roomCanonicalAliasEventContent.getJsonArray("alt_aliases") ?: JsonArray()
        val canonicalAliasEventContent = JsonObject.of(
            "content",
            roomCanonicalAliasEventContent.copy().put(
                "alt_aliases", JsonArray.of(
                    *altAliases.toMutableSet().apply { remove(deletedRoomAlias.id) }.toTypedArray()
                )
            )
        )

        return updateCanonicalAliasContent(roomCanonicalAliasEvent.id, canonicalAliasEventContent)
    }

    private suspend fun getCanonicalAliasEvent(roomId: String): RoomEvent? {
        return roomEventRepository.fetchEvents(
            mapOf(
                "room_id" to roomId,
                "type" to RoomEventNames.StateEvents.CANONICAL_ALIAS
            )
        )?.first()

    }

    private suspend fun updateCanonicalAliasContent(
        canonicalAliasId: String,
        canonicalAliasEventContent: JsonObject
    ): Future<Void> {
        val dto = UpdateRoomEventDTO(
            json = canonicalAliasEventContent,
            roomEventName = RoomEventNames.StateEvents.CANONICAL_ALIAS,
            sender = null
        )

        if (dto.validate().valid) {
            try {
                roomEventRepository.update(
                    canonicalAliasId,
                    dto
                )
                return Future.succeededFuture()
            } catch (e: Exception) {
                return Future.failedFuture(e)
            }
        } else {
            return Future.failedFuture(Exception("Invalid UpdateRoomEventDTO"))
        }
    }

    private suspend fun roomMemberCreated(message: Message<JsonObject>): Future<Void> {
        val roomMemberEventJson = message.body()
        val userId =
            roomMemberEventJson.getString("state_key") ?: return Future.failedFuture("No userId found in payload")
        val userProfile = try {
            userProfileRepository.findByUserId(userId)
        } catch (e: Exception) {
            return Future.failedFuture(e)
        }
        val eventContent = JsonObject(roomMemberEventJson.getString("content"))
        val fieldsToUpdate = userProfile!!.asJson().only("displayname", "display_name", "avatar_url")
        val dto = UpdateRoomEventDTO(
            json = JsonObject.of("content", eventContent.mergeIn(fieldsToUpdate)),
            roomEventName = RoomEventNames.StateEvents.MEMBER,
            sender = null
        )
        try {
            roomEventRepository.update(roomMemberEventJson.getString("id"), dto)
        } catch (e: Exception) {
            return Future.failedFuture(e)
        }
        return Future.succeededFuture()
    }

    companion object {
        val deploymentOptions = DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER).setInstances(3)
            .setWorkerPoolName("room-events-worker-pool")
            .setWorkerPoolSize(3)

    }
}