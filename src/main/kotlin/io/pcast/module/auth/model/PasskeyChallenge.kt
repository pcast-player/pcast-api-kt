package io.pcast.module.auth.model

import java.time.LocalDateTime
import java.util.UUID

enum class PasskeyChallengeType {
    Registration,
    Authentication,
    SignupRegistration,
}

data class PasskeyChallenge(
    val id: UUID,
    val userId: UUID?,
    val type: PasskeyChallengeType,
    val challenge: String,
    val requestJson: String,
    val email: String?,
    val passkeyUserHandle: String?,
    val expiresAt: LocalDateTime,
    val consumedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)
