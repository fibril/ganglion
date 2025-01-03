package utils

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import java.time.Duration


class PutRequestRateLimiter private constructor() {
    companion object {
        @Volatile
        private var instance: PutRequestRateLimiter? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: PutRequestRateLimiter().also { instance = it }
            }

        private val config: RateLimiterConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMillis(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(25))
            .build()

        private const val NAME = "utils.PutRequestRateLimiter"
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

    fun getLimiter(): RateLimiter = rateLimiter!!

}