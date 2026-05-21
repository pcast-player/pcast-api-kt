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
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val CLAIM_USER_ID = "userId"
private const val CLAIM_EMAIL = "email"
private const val CLAIM_TOKEN_VERSION = "tokenVersion"
private const val BCRYPT_COST = 12
private const val SECONDS_PER_MINUTE = 60L
private const val REFRESH_TOKEN_BYTES = 32
private const val HMAC_ALGORITHM = "HmacSHA256"

@Single
class AuthService(
    private val config: Configuration,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    private val hasher = BCrypt.withDefaults()
    private val verifier = BCrypt.verifyer()
    private val secureRandom = SecureRandom()

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
        val tokenHash = hmacToken(refreshToken)
        val storedToken =
            refreshTokenRepository
                .findByTokenHash(tokenHash)
                ?.takeIf { it.expiresAt.isAfter(LocalDateTime.now()) }
                ?: return null

        refreshTokenRepository.deleteByTokenHash(tokenHash)

        return userRepository.findById(storedToken.userId)?.let { generateTokenPair(it) }
    }

    fun logout(refreshToken: String): Boolean {
        val tokenHash = hmacToken(refreshToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash) ?: return false

        // Invalidate all outstanding access tokens by bumping the token version.
        userRepository.incrementTokenVersion(storedToken.userId)
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
            withClaim(CLAIM_TOKEN_VERSION, user.tokenVersion)
            withExpiresIn(config.jwt.accessTokenExpireMinutes)
        }

    private fun generateRefreshToken(user: User): String {
        // 256 bits of SecureRandom, base64url-encoded (no padding)
        val rawBytes = ByteArray(REFRESH_TOKEN_BYTES).also { secureRandom.nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes)

        val tokenHash = hmacToken(token)
        val expiresAt = LocalDateTime.now().plusDays(config.jwt.refreshTokenExpireDays)

        refreshTokenRepository.create(
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
        )

        return token
    }

    /**
     * Returns a base64url-encoded HMAC-SHA256 of [token] keyed with the JWT secret.
     * Stored in the database; the raw token is never persisted.
     */
    private fun hmacToken(token: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(config.jwt.secret.toByteArray(), HMAC_ALGORITHM))
        val hmacBytes = mac.doFinal(token.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes)
    }

    private fun hashPassword(password: String): String = hasher.hashToString(BCRYPT_COST, password.toCharArray())

    private fun verifyPassword(
        password: String,
        hash: String,
    ): Boolean = verifier.verify(password.toCharArray(), hash).verified
}
