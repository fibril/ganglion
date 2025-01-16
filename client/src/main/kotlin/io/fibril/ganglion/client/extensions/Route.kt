package io.fibril.ganglion.client.extensions

import io.fibril.ganglion.client.utils.RequestRateLimiter
import io.fibril.ganglion.client.v1.authentication.RequestAuthenticator
import io.fibril.ganglion.client.v1.authentication.UserType
import io.vertx.ext.web.Route


fun Route.addRequestRateLimiter(requestRateLimiter: RequestRateLimiter): Route {
    this.handler(requestRateLimiter::handle)
    return this
}

fun Route.authenticatedForUser(): Route {
    this.handler { context ->
        RequestAuthenticator.handleRequestAuthentication(context, UserType.USER)
    }
    return this
}

fun Route.authenticatedForAdmin(): Route {
    this.handler { context ->
        RequestAuthenticator.handleRequestAuthentication(context, UserType.ADMIN)
    }
    return this
}