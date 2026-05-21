package io.pcast.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.pcast.config.Configuration
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.module.auth.model.UserRepository
import org.koin.ktor.ext.inject
import java.util.UUID

const val JWT_AUTH_NAME = "jwt-auth"

fun Application.configureAuth() {
    val config by inject<Configuration>()
    val userRepository by inject<UserRepository>()

    install(Authentication) {
        jwt(JWT_AUTH_NAME) {
            realm = config.jwt.issuer

            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.jwt.secret))
                    .withIssuer(config.jwt.issuer)
                    .withAudience(config.jwt.audience)
                    .build(),
            )

            validate { credential ->
                val userIdStr = credential.payload.getClaim("userId")?.asString() ?: return@validate null
                val userId = runCatching { UUID.fromString(userIdStr) }.getOrNull() ?: return@validate null

                // Reject tokens whose subject no longer exists in the database.
                // This closes the window where a deleted account keeps working
                // until the token expires naturally.
                userRepository.findById(userId) ?: return@validate null

                JWTPrincipal(credential.payload)
            }

            challenge { _, _ ->
                throw AbortError(HttpError.Unauthorized, "Unauthorized")
            }
        }
    }
}
