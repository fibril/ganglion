package io.fibril.ganglion.clientServer.v1

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.v1.authentication.AuthController
import io.fibril.ganglion.clientServer.v1.authentication.AuthService
import io.fibril.ganglion.clientServer.v1.authentication.AuthServiceImpl
import io.fibril.ganglion.clientServer.v1.filters.FilterController
import io.fibril.ganglion.clientServer.v1.filters.FilterService
import io.fibril.ganglion.clientServer.v1.filters.FilterServiceImpl
import io.fibril.ganglion.clientServer.v1.media.MediaController
import io.fibril.ganglion.clientServer.v1.media.MediaService
import io.fibril.ganglion.clientServer.v1.media.MediaServiceImpl
import io.fibril.ganglion.clientServer.v1.presence.PresenceController
import io.fibril.ganglion.clientServer.v1.presence.PresenceService
import io.fibril.ganglion.clientServer.v1.presence.PresenceServiceImpl
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventServiceImpl
import io.fibril.ganglion.clientServer.v1.rooms.*
import io.fibril.ganglion.clientServer.v1.typing.TypingController
import io.fibril.ganglion.clientServer.v1.typing.TypingService
import io.fibril.ganglion.clientServer.v1.typing.TypingServiceImpl
import io.fibril.ganglion.clientServer.v1.users.*
import io.vertx.core.Vertx
import io.vertx.ext.web.Router


class RoutesV1 @Inject constructor(private val vertx: Vertx, val servicesMap: Map<String, Service<*>>) {
    val router: Router = Router.router(vertx)

    init {
        composeSubRoutes()
    }

    private fun composeSubRoutes() {
        // USER
        val usersRouter =
            UserController(vertx, servicesMap[UserServiceImpl.IDENTIFIER] as UserService).mountSubRoutes()
        val userProfileRouter =
            UserProfileController(vertx, servicesMap[UserProfileServiceImpl.IDENTIFIER] as UserProfileService)
                .mountSubRoutes()

        // MEDIA
        val mediaController = MediaController(vertx, servicesMap[MediaServiceImpl.IDENTIFIER] as MediaService)
        val mediaRouter = mediaController.mountSubRoutes()
        val mediaCreationAndUploadRouter = mediaController.mountMediaCreateAndUploadRoutes()

        // AUTHENTICATION
        val authControllerRouter =
            AuthController(vertx, servicesMap[AuthServiceImpl.IDENTIFIER] as AuthService).mountSubRoutes()

        // ROOM
        val roomControllerRouter =
            RoomController(
                vertx,
                servicesMap[RoomServiceImpl.IDENTIFIER] as RoomService,
                servicesMap[RoomEventServiceImpl.IDENTIFIER] as RoomEventService
            ).mountSubRoutes()

        // ROOM ALIAS
        val roomAliasControllerRouter =
            RoomAliasController(
                vertx,
                servicesMap[RoomAliasServiceImpl.IDENTIFIER] as RoomAliasService
            ).mountSubRoutes()

        // PRESENCE
        val presenceControllerRouter =
            PresenceController(
                vertx,
                servicesMap[PresenceServiceImpl.IDENTIFIER] as PresenceService
            ).mountSubRoutes()

        // TYPING
        val typingControllerRouter =
            TypingController(
                vertx,
                servicesMap[TypingServiceImpl.IDENTIFIER] as TypingService
            ).mountSubRoutes()

        // FILTER
        val filterControllerRouter = FilterController(
            vertx,
            servicesMap[FilterServiceImpl.IDENTIFIER] as FilterService
        ).mountSubRoutes()





        router.route(PATH_PREFIX).subRouter(usersRouter)
        router.route(PATH_PREFIX).subRouter(userProfileRouter)

        router.route(PATH_PREFIX).subRouter(mediaRouter)
        // no path prefix
        router.route().subRouter(mediaCreationAndUploadRouter)

        router.route(PATH_PREFIX).subRouter(authControllerRouter)

        router.route(PATH_PREFIX).subRouter(roomControllerRouter)

        router.route(PATH_PREFIX).subRouter(roomAliasControllerRouter)

        router.route(PATH_PREFIX).subRouter(presenceControllerRouter)

        router.route(PATH_PREFIX).subRouter(typingControllerRouter)

        router.route(PATH_PREFIX).subRouter(filterControllerRouter)

    }

    companion object {
        const val MATRIX_CLIENT_SERVER_PATH_PREFIX = "_matrix/client"
        const val PATH_PREFIX = "/$MATRIX_CLIENT_SERVER_PATH_PREFIX*"
    }
}