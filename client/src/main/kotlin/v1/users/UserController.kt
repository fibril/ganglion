package v1.users

import Controller
import DTO
import com.google.inject.Inject
import errors.ErrorCodes
import errors.RequestException
import errors.StandardErrorResponse
import extensions.only
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import utils.CoroutineHelpers
import v1.users.dtos.CreateUserDTO

internal class UserController @Inject constructor(vertx: Vertx, val userService: UserService) : Controller(vertx) {
    override fun mountSubRoutes(): Router {
        router.route().handler(BodyHandler.create());
        router.post(REGISTER_PATH).handler(::registerUser)

        router.post(USER_DIRECTORY_SEARCH_PATH).handler(::handleUserDirectorySearch)
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
                            user.asJson().only(PERMITTED_CREATE_USER_SUCCESS_RESPONSE_PARAMS).toString()
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

        }
    }

    companion object {
        const val USER_DIRECTORY_SEARCH_PATH = "/v3/user_directory/search"
        const val REGISTER_PATH = "/v3/register"

        val PERMITTED_CREATE_USER_SUCCESS_RESPONSE_PARAMS = setOf(
            "access_token",
            "device_id",
            "expires_in_ms",
            "refresh_token",
            "user_id"
        )
    }
}