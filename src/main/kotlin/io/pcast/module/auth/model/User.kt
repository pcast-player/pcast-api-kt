package io.pcast.module.auth.model

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val createdAt: LocalDateTime,
    val tokenVersion: Int = 0,
    val passkeyUserHandle: String,
)
