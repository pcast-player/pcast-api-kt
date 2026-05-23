package io.pcast.module.auth.model

import java.time.LocalDateTime
import java.util.UUID

data class PasskeyCredential(
    val id: UUID,
    val userId: UUID,
    val credentialId: String,
    val publicKeyCose: String,
    val signatureCount: Long,
    val transports: String?,
    val nickname: String?,
    val backupEligible: Boolean?,
    val backedUp: Boolean?,
    val createdAt: LocalDateTime,
    val lastUsedAt: LocalDateTime?,
)
