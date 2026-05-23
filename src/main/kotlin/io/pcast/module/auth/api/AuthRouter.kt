package io.pcast.module.auth.api

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.extensions.userId
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.passkey.PasskeyService
import io.pcast.module.auth.passkey.request.PasskeyAuthenticationOptionsRequest
import io.pcast.module.auth.passkey.response.PasskeyCredentialResponse
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.request.RefreshRequest
import io.pcast.plugins.JWT_AUTH_NAME
import io.pcast.plugins.RATE_LIMIT_LOGIN
import io.pcast.plugins.RATE_LIMIT_PASSKEY
import io.pcast.plugins.RATE_LIMIT_REFRESH
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("AuthRouter")

fun Route.registerAuthRoutes() {
    val authService by inject<AuthService>()
    val passkeyService by inject<PasskeyService>()

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

    authenticate(JWT_AUTH_NAME) {
        post("/auth/passkeys/registration/options") {
            val optionsJson = passkeyService.startRegistration(call.userId())

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respondText(optionsJson, ContentType.Application.Json, HttpStatusCode.OK)
        }

        post("/auth/passkeys/registration/finish") {
            val credential = passkeyService.finishRegistration(call.userId(), call.receiveText())

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respond(HttpStatusCode.Created, PasskeyCredentialResponse(credential))
        }

        get("/auth/passkeys") {
            val credentials = passkeyService.listCredentials(call.userId()).map(::PasskeyCredentialResponse)

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respond(HttpStatusCode.OK, credentials)
        }

        delete("/auth/passkeys/{id}") {
            val credentialId =
                call.parameters["id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: throw AbortError(HttpError.BadRequest, "Passkey credential ID must be provided")

            passkeyService.deleteCredential(call.userId(), credentialId)
            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respond(HttpStatusCode.NoContent)
        }
    }

    rateLimit(RATE_LIMIT_PASSKEY) {
        post("/auth/passkeys/authentication/options") {
            val request = call.receive<PasskeyAuthenticationOptionsRequest>()
            val optionsJson = passkeyService.startAuthentication(request.email)

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respondText(optionsJson, ContentType.Application.Json, HttpStatusCode.OK)
        }

        post("/auth/passkeys/authentication/finish") {
            val tokenResponse = passkeyService.finishAuthentication(call.receiveText())

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respond(HttpStatusCode.OK, tokenResponse)
        }
    }
}
