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
import java.time.LocalDateTime
import java.util.UUID

private const val VARCHAR_MAX_LENGTH = 255

object UsersTable : UUIDTable("users") {
    val email = varchar("email", VARCHAR_MAX_LENGTH).uniqueIndex()
    val passwordHash = varchar("password_hash", VARCHAR_MAX_LENGTH)
    val createdAt = datetime("created_at")
}

class UserRepository(
    private val db: Database,
) {
    fun findByEmail(email: String): User? =
        transaction(db) {
            UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
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

    fun create(
        email: String,
        passwordHash: String,
    ): User {
        val user =
            User(
                id = generateUuidV7(),
                email = email,
                passwordHash = passwordHash,
                createdAt = LocalDateTime.now(),
            )

        transaction(db) {
            UsersTable.insert {
                it[id] = user.id
                it[UsersTable.email] = user.email
                it[UsersTable.passwordHash] = user.passwordHash
                it[createdAt] = user.createdAt
            }
        }

        return user
    }

    private fun mapRow(row: ResultRow) =
        User(
            id = row[UsersTable.id].value,
            email = row[UsersTable.email],
            passwordHash = row[UsersTable.passwordHash],
            createdAt = row[UsersTable.createdAt],
        )
}
