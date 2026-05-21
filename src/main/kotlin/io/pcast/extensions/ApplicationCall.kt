package io.pcast.extensions

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("ApplicationCall")

/**
 * Extracts the authenticated user's ID from the JWT principal.
 * @throws AbortError with Unauthorized if no valid JWT principal exists
 */
fun ApplicationCall.userId(): UUID {
    val principal =
        principal<JWTPrincipal>()
            ?: run {
                log.warn("userId() called but no JWTPrincipal present")
                throw AbortError(HttpError.Unauthorized, "Unauthorized")
            }

    val userId =
        principal.payload.getClaim("userId")?.asString()
            ?: run {
                log.warn("JWT missing userId claim")
                throw AbortError(HttpError.Unauthorized, "Unauthorized")
            }

    return runCatching { UUID.fromString(userId) }
        .getOrElse { cause ->
            log.warn("JWT userId claim is malformed: {}", userId)
            throw AbortError(HttpError.Unauthorized, "Unauthorized", cause)
        }
}

/**
 * Extracts the authenticated user's email from the JWT principal.
 * @throws AbortError with Unauthorized if no valid JWT principal exists
 */
fun ApplicationCall.userEmail(): String {
    val principal =
        principal<JWTPrincipal>()
            ?: run {
                log.warn("userEmail() called but no JWTPrincipal present")
                throw AbortError(HttpError.Unauthorized, "Unauthorized")
            }

    return principal.payload.getClaim("email")?.asString()
        ?: run {
            log.warn("JWT missing email claim")
            throw AbortError(HttpError.Unauthorized, "Unauthorized")
        }
}
