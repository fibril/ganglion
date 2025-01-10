package io.fibril.ganglion.client.v1.authentication

import io.fibril.ganglion.client.Controller
import com.google.inject.Inject
import io.vertx.core.Vertx
import io.vertx.ext.web.Router

internal class AuthController @Inject constructor(vertx: Vertx, private val authService: AuthService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {
        return router
    }

    companion object {
    }
}