package v1

import Service
import com.google.inject.Inject
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import v1.media.MediaController
import v1.media.MediaService
import v1.media.MediaServiceImpl
import v1.users.*


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

        val mediaController = MediaController(vertx, servicesMap[MediaServiceImpl.IDENTIFIER] as MediaService)
        val mediaRouter = mediaController.mountSubRoutes()
        val mediaCreationAndUploadRouter = mediaController.mountMediaCreateAndUploadRoutes()

        router.route(PATH_PREFIX).subRouter(usersRouter)
        router.route(PATH_PREFIX).subRouter(userProfileRouter)
        router.route(PATH_PREFIX).subRouter(mediaRouter)
        // no path prefix
        router.route().subRouter(mediaCreationAndUploadRouter)

    }

    companion object {
        const val MATRIX_CLIENT_SERVER_PATH_PREFIX = "_matrix/client"
        const val PATH_PREFIX = "/${MATRIX_CLIENT_SERVER_PATH_PREFIX}*"
    }
}