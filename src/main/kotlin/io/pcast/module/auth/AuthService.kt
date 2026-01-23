package io.pcast.module.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.pcast.config.Configuration
import io.pcast.module.auth.model.RefreshTokenRepository
import io.pcast.module.auth.model.User
import io.pcast.module.auth.model.UserRepository
import io.pcast.module.auth.response.TokenResponse
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class AuthService(
    private val config: Configuration,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    fun login(
        email: String,
        password: String,
    ): TokenResponse? {
        val user =
            userRepository
                .findByEmail(email)
                ?.takeIf { verifyPassword(password, it.passwordHash) }

        return user?.let { generateTokenPair(it) }
    }

    fun refresh(refreshToken: String): TokenResponse? {
        val tokenHash = hashToken(refreshToken)
        val storedToken =
            refreshTokenRepository
                .findByTokenHash(tokenHash)
                ?.takeIf { it.expiresAt.isAfter(LocalDateTime.now()) }
                ?: return null

        refreshTokenRepository.deleteByTokenHash(tokenHash)

        return userRepository.findById(storedToken.userId)?.let { generateTokenPair(it) }
    }

    fun logout(refreshToken: String): Boolean {
        val tokenHash = hashToken(refreshToken)
        return refreshTokenRepository.deleteByTokenHash(tokenHash)
    }

    fun createUser(
        email: String,
        password: String,
    ): User = userRepository.create(email, hashPassword(password))

    private fun generateTokenPair(user: User): TokenResponse {
        val accessToken = generateAccessToken(user)
        val refreshToken = generateRefreshToken(user)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = config.jwt.accessTokenExpireMinutes * SECONDS_PER_MINUTE,
        )
    }

    private fun generateAccessToken(user: User): String {
        val expiresAt =
            Instant.now().plusSeconds(
                config.jwt.accessTokenExpireMinutes * SECONDS_PER_MINUTE,
            )

        return JWT
            .create()
            .withIssuer(config.jwt.issuer)
            .withAudience(config.jwt.audience)
            .withSubject(user.id.toString())
            .withClaim(CLAIM_USER_ID, user.id.toString())
            .withClaim(CLAIM_EMAIL, user.email)
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(config.jwt.secret))
    }

    private fun generateRefreshToken(user: User): String {
        val token = UUID.randomUUID().toString()
        val tokenHash = hashToken(token)
        val expiresAt = LocalDateTime.now().plusDays(config.jwt.refreshTokenExpireDays)

        refreshTokenRepository.create(
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
        )

        return token
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String): String =
        BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())

    private fun verifyPassword(
        password: String,
        hash: String,
    ): Boolean = BCrypt.verifyer().verify(password.toCharArray(), hash).verified

    companion object {
        const val CLAIM_USER_ID = "userId"
        const val CLAIM_EMAIL = "email"
        private const val BCRYPT_COST = 12
        private const val SECONDS_PER_MINUTE = 60L
    }
}
