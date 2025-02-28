package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.only
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.UsersRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.users.dtos.CreateUserDTO
import io.fibril.ganglion.clientServer.v1.users.dtos.UsersDirectorySearchDTO
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class UserController @Inject constructor(vertx: Vertx, val userService: UserService) : Controller(vertx) {
    override fun mountSubRoutes(): Router {
        router.route().handler(BodyHandler.create())
        router.post(REGISTER_PATH).handler(::registerUser)

        router.post(USER_DIRECTORY_SEARCH_PATH)
            .addRequestRateLimiter(UsersRequestRateLimiter.getInstance())
            .useDTOValidation(UsersDirectorySearchDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::handleUserDirectorySearch)
        return router
    }

    private fun registerUser(routingContext: RoutingContext) {
        val kind = routingContext.pathParams().get("kind")
        if (kind?.trim() == "guest") {
            routingContext.response().setStatusCode(403)
            routingContext.end(StandardErrorResponse(ErrorCodes.M_GUEST_ACCESS_FORBIDDEN).toString())
            return
        }

        val registerUserDTO = CreateUserDTO(routingContext.body().asJsonObject())
        DTO.Helpers.useDTOValidation(registerUserDTO, routingContext) {
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                userService.create(registerUserDTO)
                    .onSuccess { user ->
                        routingContext.end(
                            user.asJson()
                                .only(*PERMITTED_CREATE_USER_SUCCESS_RESPONSE_PARAMS).toString()
                        )
                    }
                    .onFailure {
                        val err = it as RequestException
                        routingContext.response().setStatusCode(err.statusCode)
                        routingContext.end(err.json.toString())
                    }
            }
        }
    }

    private fun handleUserDirectorySearch(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val json = JsonObject().apply {
                for (entry in routingContext.queryParams()) {
                    put(entry.key, entry.value)
                }
            }.mergeIn(routingContext.body()?.asJsonObject() ?: JsonObject())
            val dto = UsersDirectorySearchDTO(json)
            userService.userDirectorySearch(dto)
                .onSuccess {
                    val results = it.chunk.map { it.asJson().only("avatar_url", "display_name", "user_id") }
                    val limited = it.next_batch != null
                    routingContext.end(JsonObject.of("results", results, "limited", limited).toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }

    }

    companion object {
        const val USER_DIRECTORY_SEARCH_PATH = "/v3/user_directory/search"
        const val REGISTER_PATH = "/v3/register"

        val PERMITTED_CREATE_USER_SUCCESS_RESPONSE_PARAMS = arrayOf(
            "access_token",
            "device_id",
            "expires_in_ms",
            "refresh_token",
            "user_id"
        )
    }
}