package io.pcast.extensions

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import java.util.UUID

/**
 * Extracts the authenticated user's ID from the JWT principal.
 * @throws AbortError with Unauthorized if no valid JWT principal exists
 */
fun ApplicationCall.userId(): UUID {
    val principal =
        principal<JWTPrincipal>()
            ?: throw AbortError(HttpError.Unauthorized, "Not authenticated")

    val userId =
        principal.payload.getClaim("userId")?.asString()
            ?: throw AbortError(HttpError.Unauthorized, "Invalid token: missing userId claim")

    return runCatching { UUID.fromString(userId) }
        .getOrElse { cause ->
            throw AbortError(HttpError.Unauthorized, "Invalid token: malformed userId", cause)
        }
}

/**
 * Extracts the authenticated user's email from the JWT principal.
 * @throws AbortError with Unauthorized if no valid JWT principal exists
 */
fun ApplicationCall.userEmail(): String {
    val principal =
        principal<JWTPrincipal>()
            ?: throw AbortError(HttpError.Unauthorized, "Not authenticated")

    return principal.payload.getClaim("email")?.asString()
        ?: throw AbortError(HttpError.Unauthorized, "Invalid token: missing email claim")
}
