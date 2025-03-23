package io.fibril.ganglion.clientServer.v1.presence

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.PresenceRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.presence.dtos.GetPresenceDTO
import io.fibril.ganglion.clientServer.v1.presence.dtos.PutPresenceDTO
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class PresenceController @Inject constructor(vertx: Vertx, val presenceService: PresenceService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        router.route().handler(BodyHandler.create())
        
        router.get(GET_USER_PRESENCE_PATH)
            .useDTOValidation(GetPresenceDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::getUserPresence)

        router.put(PUT_USER_PRESENCE_PATH)
            .addRequestRateLimiter(PresenceRequestRateLimiter.getInstance())
            .useDTOValidation(PutPresenceDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::putUserPresence)
        return router
    }

    private fun getUserPresence(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val userId = routingContext.request().params().get("userId")
            presenceService.findOne(userId)
                .onSuccess { presence ->
                    if (presence != null) {
                        routingContext.end(presence.asJson().getJsonObject("content")?.toString())
                    } else {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }

    }

    private fun putUserPresence(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val userId = routingContext.request().params().get("userId")
            val senderId = routingContext.user().principal()?.getString("sub")
            if (userId != senderId) {
                routingContext.response().setStatusCode(403)
                routingContext.end(StandardErrorResponse(ErrorCodes.M_FORBIDDEN).asJson().toString())
                return@usingCoroutineScopeWithIODispatcher
            }
            val putPresenceDTO = PutPresenceDTO(
                JsonObject.mapFrom(routingContext.request().params()).mergeIn(routingContext.body().asJsonObject()),
                routingContext.user()
            )
            presenceService.update(userId, putPresenceDTO)
                .onSuccess { _ ->
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    companion object {
        const val GET_USER_PRESENCE_PATH = "/v3/presence/:userId/status"
        const val PUT_USER_PRESENCE_PATH = "/v3/presence/:userId/status"
    }
}