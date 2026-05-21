package io.pcast.module.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import io.pcast.config.Configuration
import io.pcast.extensions.jwt
import io.pcast.extensions.withExpiresIn
import io.pcast.module.auth.model.RefreshTokenRepository
import io.pcast.module.auth.model.User
import io.pcast.module.auth.model.UserRepository
import io.pcast.module.auth.response.TokenResponse
import org.koin.core.annotation.Single
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID

private const val CLAIM_USER_ID = "userId"
private const val CLAIM_EMAIL = "email"
private const val BCRYPT_COST = 12
private const val SECONDS_PER_MINUTE = 60L
private const val DIGEST_ALGORITHM = "SHA-256"

@Single
class AuthService(
    private val config: Configuration,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    private val hasher = BCrypt.withDefaults()
    private val verifier = BCrypt.verifyer()

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

    fun getUserByEmail(email: String): User? = userRepository.findByEmail(email)

    private fun generateTokenPair(user: User): TokenResponse {
        val accessToken = generateAccessToken(user)
        val refreshToken = generateRefreshToken(user)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = config.jwt.accessTokenExpireMinutes * SECONDS_PER_MINUTE,
        )
    }

    private fun generateAccessToken(user: User): String =
        jwt(config.jwt.secret) {
            withIssuer(config.jwt.issuer)
            withAudience(config.jwt.audience)
            withSubject(user.id.toString())
            withClaim(CLAIM_USER_ID, user.id.toString())
            withClaim(CLAIM_EMAIL, user.email)
            withExpiresIn(config.jwt.accessTokenExpireMinutes)
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
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        val hashBytes = digest.digest(token.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String): String = hasher.hashToString(BCRYPT_COST, password.toCharArray())

    private fun verifyPassword(
        password: String,
        hash: String,
    ): Boolean = verifier.verify(password.toCharArray(), hash).verified
}
