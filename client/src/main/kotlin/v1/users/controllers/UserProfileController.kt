package v1.users.controllers

import Controller
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

internal class UserProfileController(vertx: Vertx) : Controller(vertx) {
    override fun mountRoutes(): Router {
        router.get(USER_PROFILE_PATH).handler(::getUserProfile)
        router.get(USER_AVATAR_URL_PATH).handler(::getUserAvatarUrl)

        router.put(USER_AVATAR_URL_PATH).handler(::putUserAvatarUrl)

        return router
    }

    private fun getUserProfile(context: RoutingContext) {

    }

    private fun getUserAvatarUrl(context: RoutingContext) {

    }

    private fun putUserAvatarUrl(context: RoutingContext) {

    }

    companion object {
        const val USER_PROFILE_PATH = "/_matrix/client/v3/profile/:userId"
        const val USER_AVATAR_URL_PATH = "/_matrix/client/v3/profile/:userId/avatar_url"
    }
}