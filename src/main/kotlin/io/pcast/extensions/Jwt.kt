package io.pcast.extensions

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant

private const val SECONDS_PER_MINUTE = 60L

fun jwt(
    secret: String,
    algorithm: (String) -> Algorithm = { Algorithm.HMAC256(it) },
    builder: JWTCreator.Builder.() -> Unit,
): String =
    JWT
        .create()
        .apply(builder)
        .sign(algorithm(secret))

fun JWTCreator.Builder.withExpiresIn(minutes: Long): JWTCreator.Builder =
    withExpiresAt(
        Instant.now().plusSeconds(minutes * SECONDS_PER_MINUTE),
    )
