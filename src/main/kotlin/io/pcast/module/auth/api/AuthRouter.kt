package io.pcast.module.auth.api

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.request.RefreshRequest
import io.pcast.plugins.RATE_LIMIT_LOGIN
import io.pcast.plugins.RATE_LIMIT_REFRESH
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AuthRouter")

fun Route.registerAuthRoutes() {
    val authService by inject<AuthService>()

    rateLimit(RATE_LIMIT_LOGIN) {
        post("/auth/login") {
            val request = call.receive<LoginRequest>()

            val tokenResponse =
                authService.login(request.email, request.password)
                    ?: run {
                        log.warn("Login failed for email={}", request.email)
                        throw AbortError(HttpError.Unauthorized, "Invalid email or password")
                    }

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respond(HttpStatusCode.OK, tokenResponse)
        }
    }

    rateLimit(RATE_LIMIT_REFRESH) {
        post("/auth/refresh") {
            val request = call.receive<RefreshRequest>()

            val tokenResponse =
                authService.refresh(request.refreshToken)
                    ?: run {
                        log.warn("Refresh token not found or expired")
                        throw AbortError(HttpError.Unauthorized, "Unauthorized")
                    }

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respond(HttpStatusCode.OK, tokenResponse)
        }
    }

    post("/auth/logout") {
        val request = call.receive<RefreshRequest>()

        authService.logout(request.refreshToken)

        call.response.header(HttpHeaders.CacheControl, "no-store")
        call.respond(HttpStatusCode.NoContent)
    }
}
