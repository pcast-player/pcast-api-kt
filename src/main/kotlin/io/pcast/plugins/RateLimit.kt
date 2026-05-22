package io.pcast.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

/**
 * Rate-limit names referenced by the auth router.
 *
 * NOTE: These limits are in-memory and apply per-instance. When running
 * multiple replicas behind a load balancer each instance maintains an
 * independent counter. For horizontal-scale deployments, replace the
 * in-memory store with a shared Redis-backed limiter.
 */
val RATE_LIMIT_LOGIN = RateLimitName("login")
val RATE_LIMIT_REFRESH = RateLimitName("refresh")

fun Application.configureRateLimit() {
    install(RateLimit) {
        // Login: 5 attempts per minute per IP address.
        // bcrypt at cost 12 provides ~200ms natural throttling per attempt,
        // but a distributed attack across many IPs still warrants a hard limit.
        register(RATE_LIMIT_LOGIN) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }

        // Refresh: 30 attempts per minute per IP address.
        register(RATE_LIMIT_REFRESH) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }
    }
}
