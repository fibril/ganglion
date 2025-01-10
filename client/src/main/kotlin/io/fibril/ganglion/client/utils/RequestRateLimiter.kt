package io.fibril.ganglion.client.utils

import io.fibril.ganglion.client.errors.ErrorCodes
import io.fibril.ganglion.client.errors.StandardErrorResponse
import io.github.resilience4j.ratelimiter.RateLimiter
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext

abstract class RequestRateLimiter {
    abstract fun getLimiter(): RateLimiter

    /**
     * Return a value defined in a companion object in order to
     * maintain initiation order of derived and abstract classes.
     */
    abstract val retryAfterMs: Long

    private fun handleNext(routingContext: RoutingContext) = routingContext.next()

    private val requestMonitoringCall: Runnable by lazy {
        RateLimiter.decorateRunnable(
            getLimiter(),
        ) {}
    }


    fun handle(routingContext: RoutingContext): Result<Unit> {
        return runCatching {
            requestMonitoringCall.run()
        }.onSuccess {
            handleNext(routingContext)
        }.onFailure {
            routingContext.response()
                .setStatusCode(429)
                .putHeader("Retry-After", retryAfterMs.toString())
                .end(
                    StandardErrorResponse(
                        ErrorCodes.M_LIMIT_EXCEEDED,
                        JsonObject().put("retry_after_ms", retryAfterMs)
                    ).toString()
                )
        }
    }

    fun useRateLimiting(route: Route, routeHandler: (ctx: RoutingContext) -> Unit) {
        route.handler(::handle)
        route.handler(routeHandler)
    }

}