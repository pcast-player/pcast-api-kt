package io.pcast.extensions

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant

private const val SECONDS_PER_MINUTE = 60L

/**
 * Creates and signs a JWT using HMAC256 with [secret].
 * The algorithm is intentionally hardcoded to prevent algorithm-confusion
 * attacks (e.g. alg=none or RS256→HS256 substitution).
 */
fun jwt(
    secret: String,
    builder: JWTCreator.Builder.() -> Unit,
): String =
    JWT
        .create()
        .apply(builder)
        .sign(Algorithm.HMAC256(secret))

fun JWTCreator.Builder.withExpiresIn(minutes: Long): JWTCreator.Builder =
    withExpiresAt(
        Instant.now().plusSeconds(minutes * SECONDS_PER_MINUTE),
    )
