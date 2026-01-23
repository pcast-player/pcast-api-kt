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
import org.koin.ktor.ext.inject

const val JWT_AUTH_NAME = "jwt-auth"

fun Application.configureAuth() {
    val config by inject<Configuration>()

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
                val userId = credential.payload.getClaim("userId")?.asString()
                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                throw AbortError(HttpError.Unauthorized, "Invalid or expired token")
            }
        }
    }
}
