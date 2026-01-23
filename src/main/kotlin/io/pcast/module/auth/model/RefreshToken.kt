package io.pcast.module.auth.model

import java.time.LocalDateTime
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
