package io.fibril.ganglion.clientServer

import io.vertx.core.Vertx
import io.vertx.ext.web.Router

internal abstract class Controller(vertx: Vertx) {
    internal val router: Router = Router.router(vertx)

    /**
     * Mount the routes specific to this Controller
     */
    abstract fun mountSubRoutes(): Router
}