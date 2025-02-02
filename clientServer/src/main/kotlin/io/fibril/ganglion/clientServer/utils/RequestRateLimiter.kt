package io.fibril.ganglion.clientServer.utils

import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

abstract class RequestRateLimiter(private val name: String, private val rateLimiterConfig: RateLimiterConfig) {

    private fun handleNext(routingContext: RoutingContext) = routingContext.next()

    private val requestMonitoringCall: Runnable by lazy {
        RateLimiter.decorateRunnable(
            getLimiter(),
        ) {}
    }

    private var rateLimiter: RateLimiter? = null
        get() {
            if (field == null) {
                val rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig)
                return rateLimiterRegistry
                    .rateLimiter(name, rateLimiterConfig);
            }
            return field
        }


    fun getLimiter(): RateLimiter = rateLimiter!!

    fun handle(routingContext: RoutingContext): Result<Unit> {
        return runCatching {
            requestMonitoringCall.run()
        }.onSuccess {
            handleNext(routingContext)
        }.onFailure {
            routingContext.response()
                .setStatusCode(429)
                .putHeader("Retry-After", rateLimiterConfig.limitRefreshPeriod.toMillis().toString())
                .end(
                    StandardErrorResponse(
                        ErrorCodes.M_LIMIT_EXCEEDED,
                        JsonObject().put("retry_after_ms", rateLimiterConfig.limitRefreshPeriod.toMillis())
                    ).toString()
                )
        }
    }

}