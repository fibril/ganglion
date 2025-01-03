package v1.users.controllers

import Controller
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

internal class UserController(vertx: Vertx) : Controller(vertx) {
    override fun mountRoutes(): Router {
        router.post(USER_DIRECTORY_SEARCH_PATH).handler(::handleUserDirectorySearch)
        return router
    }

    private fun handleUserDirectorySearch(context: RoutingContext) {

    }

    companion object {
        const val USER_DIRECTORY_SEARCH_PATH = "/_matrix/client/v3/user_directory/search"
    }
}