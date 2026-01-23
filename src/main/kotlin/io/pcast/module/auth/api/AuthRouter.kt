package io.pcast.module.auth.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.request.RefreshRequest
import org.koin.ktor.ext.inject

fun Route.registerAuthRoutes() {
    val authService by inject<AuthService>()

    post("/auth/login") {
        val request = call.receive<LoginRequest>()

        val tokenResponse =
            authService.login(request.email, request.password)
                ?: throw AbortError(HttpError.Unauthorized, "Invalid email or password")

        call.respond(HttpStatusCode.OK, tokenResponse)
    }

    post("/auth/refresh") {
        val request = call.receive<RefreshRequest>()

        val tokenResponse =
            authService.refresh(request.refreshToken)
                ?: throw AbortError(HttpError.Unauthorized, "Invalid or expired refresh token")

        call.respond(HttpStatusCode.OK, tokenResponse)
    }

    post("/auth/logout") {
        val request = call.receive<RefreshRequest>()

        authService.logout(request.refreshToken)

        call.respond(HttpStatusCode.NoContent)
    }
}
