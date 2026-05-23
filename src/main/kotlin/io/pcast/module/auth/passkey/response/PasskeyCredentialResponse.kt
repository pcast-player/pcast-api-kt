package io.pcast.module.auth.passkey.response

import io.pcast.module.auth.model.PasskeyCredential
import io.pcast.serializer.LocalDateTimeSerializer
import io.pcast.serializer.UuidSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class PasskeyCredentialResponse(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val credentialId: String,
    val transports: String?,
    val nickname: String?,
    val backupEligible: Boolean?,
    val backedUp: Boolean?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUsedAt: LocalDateTime?,
) {
    constructor(credential: PasskeyCredential) : this(
        id = credential.id,
        credentialId = credential.credentialId,
        transports = credential.transports,
        nickname = credential.nickname,
        backupEligible = credential.backupEligible,
        backedUp = credential.backedUp,
        createdAt = credential.createdAt,
        lastUsedAt = credential.lastUsedAt,
    )
}
