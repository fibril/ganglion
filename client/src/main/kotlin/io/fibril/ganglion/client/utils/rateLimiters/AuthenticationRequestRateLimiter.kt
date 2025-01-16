package io.fibril.ganglion.client.utils.rateLimiters

import io.fibril.ganglion.client.utils.RequestRateLimiter
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import java.time.Duration


class AuthenticationRequestRateLimiter private constructor() : RequestRateLimiter() {
    companion object {
        @Volatile
        private var instance: AuthenticationRequestRateLimiter? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: AuthenticationRequestRateLimiter().also { instance = it }
            }

        val RETRY_AFTER_MS = Duration.ofSeconds(10).toMillis()

        private val config: RateLimiterConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMillis(RETRY_AFTER_MS))
            .limitForPeriod(2)
            .timeoutDuration(Duration.ofMillis(25))
            .build()

        private const val NAME = "utils.rateLimiters.AuthenticationRequestRateLimiter"

    }


    private var rateLimiter: RateLimiter? = null
        get() {
            if (field == null) {
                val rateLimiterRegistry = RateLimiterRegistry.of(config)
                return rateLimiterRegistry
                    .rateLimiter(NAME, config);
            }
            return field
        }


    override fun getLimiter(): RateLimiter = rateLimiter!!

    override val retryAfterMs = RETRY_AFTER_MS

}