package io.fibril.ganglion.clientServer.utils.rateLimiters

import io.fibril.ganglion.clientServer.utils.RequestRateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import java.time.Duration


private val RETRY_AFTER_MS = Duration.ofSeconds(10).toMillis()

private const val NAME = "utils.rateLimiters.PresenceRequestRateLimiter"

private val config: RateLimiterConfig = RateLimiterConfig.custom()
    .limitRefreshPeriod(Duration.ofMillis(RETRY_AFTER_MS))
    .limitForPeriod(2)
    .timeoutDuration(Duration.ofMillis(25))
    .build()

class PresenceRequestRateLimiter private constructor() :
    RequestRateLimiter(NAME, config) {
    companion object {
        @Volatile
        private var instance: PresenceRequestRateLimiter? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: PresenceRequestRateLimiter().also { instance = it }
            }
    }
}