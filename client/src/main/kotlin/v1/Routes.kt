package v1

import Service
import com.google.inject.Inject
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import v1.users.controllers.UserController
import v1.users.controllers.UserProfileController
import v1.users.services.UserProfileService
import v1.users.services.UserProfileServiceImpl
import v1.users.services.UserService
import v1.users.services.UserServiceImpl


class RoutesV1 @Inject constructor(private val vertx: Vertx, val servicesMap: Map<String, Service<*>>) {
    val router: Router = Router.router(vertx)

    init {
        composeSubRoutes()
    }

    private fun composeSubRoutes() {
        val usersRouter =
            UserController(vertx, servicesMap[UserServiceImpl.IDENTIFIER] as UserService).mountSubRoutes()
        val userProfileRouter =
            UserProfileController(vertx, servicesMap[UserProfileServiceImpl.IDENTIFIER] as UserProfileService)
                .mountSubRoutes()

        router.route(PATH_PREFIX).subRouter(usersRouter)
        router.route(PATH_PREFIX).subRouter(userProfileRouter)

    }

    companion object {
        const val VERSION = "v1"
        const val MATRIX_CLIENT_SERVER_PATH_PREFIX = "_matrix/client/v3"
        const val PATH_PREFIX = "/$VERSION/${MATRIX_CLIENT_SERVER_PATH_PREFIX}*"
    }
}