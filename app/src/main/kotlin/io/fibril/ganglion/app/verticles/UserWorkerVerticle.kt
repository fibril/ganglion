package io.fibril.ganglion.app.verticles

import com.google.inject.Guice
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.roomEvents.dtos.UpdateRoomEventDTO
import io.fibril.ganglion.clientServer.v1.users.UserProfileService
import io.fibril.ganglion.clientServer.v1.users.UserService
import io.fibril.ganglion.clientServer.v1.users.models.UserDatabaseActions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.ThreadingModel
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.future.await

class UserWorkerVerticle : CoroutineVerticle() {
    private lateinit var userService: UserService
    private lateinit var userProfileService: UserProfileService
    private lateinit var roomEventService: RoomEventService

    override suspend fun start() {
        val injector = Guice.createInjector(ClientModule(vertx))
        userService = injector.getInstance(UserService::class.java)
        userProfileService = injector.getInstance(UserProfileService::class.java)
        roomEventService = injector.getInstance(RoomEventService::class.java)

        val eventBus = vertx.eventBus()

        eventBus.consumer(UserDatabaseActions.USER_DISPLAY_NAME_CHANGED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                userDisplayNameChanged(message)
                    .onSuccess { isSaved ->
                        println(if (isSaved) "Token Saved" else "Unable to save token")
                    }
                    .onFailure { e ->
                        println("tokenCreated Failed from Verticle ${e.message}")
                    }
            }

        }
    }

    private suspend fun userDisplayNameChanged(message: Message<JsonObject>): Future<Boolean> {
        try {
            val body = message.body()
            val userId = body.getString("user_id")
            val displayName = body.getString("display_name")
            val userMembershipEvents = roomEventService.fetchEvents(
                mapOf(
                    "type" to RoomEventNames.StateEvents.MEMBER,
                    "state_key" to userId
                )
            ).toCompletionStage().await() ?: listOf()
            for (membershipEvent in userMembershipEvents) {
                roomEventService.update(
                    membershipEvent.id,
                    UpdateRoomEventDTO(
                        json = JsonObject.of(
                            "content",
                            JsonObject(membershipEvent.asJson().getString("content")).mergeIn(
                                JsonObject.of("display_name", displayName, "displayname", displayName)
                            )
                        ),
                        roomEventName = RoomEventNames.StateEvents.MEMBER,
                        sender = null
                    )
                ).toCompletionStage().await()
            }
            return Future.succeededFuture(true)
        } catch (e: Exception) {
            return Future.failedFuture(e)
        }
    }

    companion object {
        val deploymentOptions = DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER).setInstances(2)
            .setWorkerPoolName("user-worker-pool")
            .setWorkerPoolSize(2)

    }
}