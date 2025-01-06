package v1.users.controllers

import Controller
import com.google.inject.Inject
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import v1.users.services.UserService

internal class UserController @Inject constructor(vertx: Vertx, val userService: UserService) : Controller(vertx) {
    override fun mountSubRoutes(): Router {
        router.post(USER_DIRECTORY_SEARCH_PATH).handler(::handleUserDirectorySearch)
        return router
    }

    private fun handleUserDirectorySearch(routingContext: RoutingContext) {
        CoroutineScope(Dispatchers.IO).launch {
            routingContext.end("Not yet Implemented")
        }

    }

    companion object {
        const val USER_DIRECTORY_SEARCH_PATH = "/user_directory/search"
    }
}