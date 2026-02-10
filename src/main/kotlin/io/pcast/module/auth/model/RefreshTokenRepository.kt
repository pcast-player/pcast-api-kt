package io.pcast.module.auth.model

import io.pcast.helpers.generateUuidV7
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.util.UUID

private const val VARCHAR_MAX_LENGTH = 255

object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = varchar("token_hash", VARCHAR_MAX_LENGTH).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at")
}

@Single
class RefreshTokenRepository(
    private val db: Database,
) {
    fun create(
        userId: UUID,
        tokenHash: String,
        expiresAt: LocalDateTime,
    ): RefreshToken {
        val token =
            RefreshToken(
                id = generateUuidV7(),
                userId = userId,
                tokenHash = tokenHash,
                expiresAt = expiresAt,
                createdAt = LocalDateTime.now(),
            )

        transaction(db) {
            RefreshTokensTable.insert {
                it[id] = token.id
                it[RefreshTokensTable.userId] = token.userId
                it[RefreshTokensTable.tokenHash] = token.tokenHash
                it[RefreshTokensTable.expiresAt] = token.expiresAt
                it[createdAt] = token.createdAt
            }
        }

        return token
    }

    fun findByTokenHash(tokenHash: String): RefreshToken? =
        transaction(db) {
            RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.tokenHash eq tokenHash }
                .map(::mapRow)
                .singleOrNull()
        }

    fun deleteByTokenHash(tokenHash: String): Boolean =
        transaction(db) {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.tokenHash eq tokenHash } > 0
        }

    fun deleteAllForUser(userId: UUID): Int =
        transaction(db) {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        }

    private fun mapRow(row: ResultRow) =
        RefreshToken(
            id = row[RefreshTokensTable.id].value,
            userId = row[RefreshTokensTable.userId].value,
            tokenHash = row[RefreshTokensTable.tokenHash],
            expiresAt = row[RefreshTokensTable.expiresAt],
            createdAt = row[RefreshTokensTable.createdAt],
        )
}
