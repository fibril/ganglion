package io.fibril.ganglion.clientServer.v1.authentication

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.AuthenticationRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.dtos.LoginDTO
import io.fibril.ganglion.clientServer.v1.authentication.dtos.RefreshDTO
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class AuthController @Inject constructor(vertx: Vertx, private val authService: AuthService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {
        router.route().handler(BodyHandler.create())

        router.get(GET_LOGIN_TYPES_PATH)
            .addRequestRateLimiter(AuthenticationRequestRateLimiter.getInstance())
            .handler(::getLoginTypes)

        router.post(LOGIN_PATH)
            .addRequestRateLimiter(AuthenticationRequestRateLimiter.getInstance())
            .useDTOValidation(LoginDTO::class.java)
            .handler(::login)

        router.post(REFRESH_PATH)
            .addRequestRateLimiter(AuthenticationRequestRateLimiter.getInstance())
            .useDTOValidation(RefreshDTO::class.java)
            .handler(::refreshAuthToken)

        router.post(LOGOUT_PATH)
            .authenticatedRoute(RoleType.USER)
            .handler(::logout)

        router.post(LOGOUT_ALL_PATH)
            .authenticatedRoute(RoleType.USER)
            .handler(::logoutAll)

        return router
    }

    private fun getLoginTypes(routingContext: RoutingContext) {
        routingContext.response().end(supportedLoginTypesJson.toString())
    }

    private fun login(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val loginDTO = LoginDTO(routingContext.body().asJsonObject())
            authService.login(loginDTO)
                .onSuccess { json ->
                    routingContext.end(
                        json.toString()
                    )
                }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }

    }

    private fun refreshAuthToken(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val refreshDTO = RefreshDTO(routingContext.body().asJsonObject())
            authService.refresh(refreshDTO)
                .onSuccess { json ->
                    routingContext.end(
                        json.toString()
                    )
                }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun logout(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            authService.logout(routingContext.user())
                .onSuccess { routingContext.end(JsonObject().toString()) }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun logoutAll(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            authService.logoutAll(routingContext.user())
                .onSuccess { routingContext.end(JsonObject().toString()) }
                .onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    companion object {
        const val GET_LOGIN_TYPES_PATH = "/v3/login"
        const val LOGIN_PATH = "/v3/login"
        const val REFRESH_PATH = "/v3/refresh"
        const val LOGOUT_PATH = "/v3/logout"
        const val LOGOUT_ALL_PATH = "/v3/logout/all"

        val supportedLoginTypes = listOf(
            "m.login.password"
        )

        val supportedLoginTypesJson = JsonObject.of(
            "flows",
            JsonArray().apply {
                for (type in supportedLoginTypes) {
                    this.add(JsonObject.of("type", type))
                }
            })
    }
}