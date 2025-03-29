package io.fibril.ganglion.clientServer.v1.typing

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.TypingRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.typing.dtos.PutTypingDTO
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class TypingController @Inject constructor(vertx: Vertx, val typingService: TypingService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        router.route().handler(BodyHandler.create())

        router.put(PUT_USER_PRESENCE_PATH)
            .addRequestRateLimiter(TypingRequestRateLimiter.getInstance())
            .useDTOValidation(PutTypingDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::putUserTyping)
        return router
    }

    private fun putUserTyping(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val params = routingContext.request().params()
            val body = routingContext.body().asJsonObject()
            val roomId = params.get("roomId")
            val dto = PutTypingDTO(JsonObject.mapFrom(params).mergeIn(body))
            typingService.update(roomId, dto)
                .onSuccess {
                    routingContext.end(JsonObject().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }

    }

    companion object {
        const val PUT_USER_PRESENCE_PATH = "/v3/rooms/:roomId/typing/:userId"
    }
}