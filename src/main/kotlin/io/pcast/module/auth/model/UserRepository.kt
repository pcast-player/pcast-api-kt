package io.pcast.module.auth.model

import io.pcast.helpers.generateUuidV7
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

private const val VARCHAR_MAX_LENGTH = 255
private const val USER_HANDLE_BYTES = 64

object UsersTable : UUIDTable("users") {
    val email = varchar("email", VARCHAR_MAX_LENGTH).uniqueIndex()
    val passwordHash = varchar("password_hash", VARCHAR_MAX_LENGTH).nullable()
    val createdAt = datetime("created_at")
    val tokenVersion = integer("token_version").default(0)
    val passkeyUserHandle = varchar("passkey_user_handle", VARCHAR_MAX_LENGTH).uniqueIndex()
}

@Single
class UserRepository(
    private val db: Database,
) {
    private val secureRandom = SecureRandom()

    fun findByEmail(email: String): User? =
        transaction(db) {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email.normalizedEmail() }
                .map(::mapRow)
                .singleOrNull()
        }

    fun findById(id: UUID): User? =
        transaction(db) {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .map(::mapRow)
                .singleOrNull()
        }

    fun findByPasskeyUserHandle(userHandle: String): User? =
        transaction(db) {
            UsersTable
                .selectAll()
                .where { UsersTable.passkeyUserHandle eq userHandle }
                .map(::mapRow)
                .singleOrNull()
        }

    fun create(
        email: String,
        passwordHash: String,
        passkeyUserHandle: String = generateUserHandle(),
    ): User {
        val user =
            User(
                id = generateUuidV7(),
                email = email.normalizedEmail(),
                passwordHash = passwordHash,
                createdAt = LocalDateTime.now(),
                tokenVersion = 0,
                passkeyUserHandle = passkeyUserHandle,
            )

        transaction(db) {
            UsersTable.insert {
                it[id] = user.id
                it[UsersTable.email] = user.email
                it[UsersTable.passwordHash] = user.passwordHash
                it[createdAt] = user.createdAt
                it[tokenVersion] = user.tokenVersion
                it[UsersTable.passkeyUserHandle] = user.passkeyUserHandle
            }
        }

        return user
    }

    fun createPasskeyOnly(
        email: String,
        passkeyUserHandle: String,
    ): User {
        val user =
            User(
                id = generateUuidV7(),
                email = email.normalizedEmail(),
                passwordHash = null,
                createdAt = LocalDateTime.now(),
                tokenVersion = 0,
                passkeyUserHandle = passkeyUserHandle,
            )

        transaction(db) {
            UsersTable.insert {
                it[id] = user.id
                it[UsersTable.email] = user.email
                it[UsersTable.passwordHash] = user.passwordHash
                it[createdAt] = user.createdAt
                it[tokenVersion] = user.tokenVersion
                it[UsersTable.passkeyUserHandle] = user.passkeyUserHandle
            }
        }

        return user
    }

    /**
     * Increments token_version for the given user, immediately invalidating
     * all outstanding access tokens issued with the previous version.
     */
    fun incrementTokenVersion(userId: UUID) {
        transaction(db) {
            val current =
                UsersTable
                    .selectAll()
                    .where { UsersTable.id eq userId }
                    .singleOrNull()
                    ?.get(UsersTable.tokenVersion) ?: 0
            UsersTable.update({ UsersTable.id eq userId }) {
                it[tokenVersion] = current + 1
            }
        }
    }

    fun generateUserHandle(): String {
        val bytes = ByteArray(USER_HANDLE_BYTES).also { secureRandom.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun String.normalizedEmail(): String = trim().lowercase()

    private fun mapRow(row: ResultRow) =
        User(
            id = row[UsersTable.id].value,
            email = row[UsersTable.email],
            passwordHash = row[UsersTable.passwordHash],
            createdAt = row[UsersTable.createdAt],
            tokenVersion = row[UsersTable.tokenVersion],
            passkeyUserHandle = row[UsersTable.passkeyUserHandle],
        )
}
