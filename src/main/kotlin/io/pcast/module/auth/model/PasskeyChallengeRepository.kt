package io.pcast.module.auth.model

import io.pcast.helpers.generateUuidV7
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.updateReturning
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.util.UUID

private const val CHALLENGE_TYPE_MAX_LENGTH = 32
private const val CHALLENGE_MAX_LENGTH = 512

object PasskeyChallengesTable : UUIDTable("passkey_challenges") {
    val userId = reference("user_id", UsersTable).nullable()
    val type = varchar("type", CHALLENGE_TYPE_MAX_LENGTH)
    val challenge = varchar("challenge", CHALLENGE_MAX_LENGTH).uniqueIndex()
    val requestJson = text("request_json")
    val expiresAt = datetime("expires_at")
    val consumedAt = datetime("consumed_at").nullable()
    val createdAt = datetime("created_at")
}

@Single
class PasskeyChallengeRepository(
    private val db: Database,
) {
    fun create(
        userId: UUID?,
        type: PasskeyChallengeType,
        challenge: String,
        requestJson: String,
        expiresAt: LocalDateTime,
    ): PasskeyChallenge {
        val pending =
            PasskeyChallenge(
                id = generateUuidV7(),
                userId = userId,
                type = type,
                challenge = challenge,
                requestJson = requestJson,
                expiresAt = expiresAt,
                consumedAt = null,
                createdAt = LocalDateTime.now(),
            )

        purgeStale(LocalDateTime.now())

        transaction(db) {
            PasskeyChallengesTable.insert {
                it[id] = pending.id
                it[PasskeyChallengesTable.userId] = pending.userId
                it[PasskeyChallengesTable.type] = pending.type.name
                it[PasskeyChallengesTable.challenge] = pending.challenge
                it[PasskeyChallengesTable.requestJson] = pending.requestJson
                it[PasskeyChallengesTable.expiresAt] = pending.expiresAt
                it[consumedAt] = pending.consumedAt
                it[createdAt] = pending.createdAt
            }
        }

        return pending
    }

    fun consume(
        challenge: String,
        type: PasskeyChallengeType,
    ): PasskeyChallenge? =
        transaction(db) {
            val now = LocalDateTime.now()

            PasskeyChallengesTable
                .updateReturning(
                    returning = PasskeyChallengesTable.columns,
                    where = {
                        (PasskeyChallengesTable.challenge eq challenge) and
                            (PasskeyChallengesTable.type eq type.name) and
                            PasskeyChallengesTable.consumedAt.isNull() and
                            (PasskeyChallengesTable.expiresAt greater now)
                    },
                ) {
                    it[consumedAt] = now
                }.map(::mapRow)
                .singleOrNull()
        }

    fun purgeStale(now: LocalDateTime): Int =
        transaction(db) {
            PasskeyChallengesTable.deleteWhere {
                (expiresAt lessEq now) or consumedAt.isNotNull()
            }
        }

    private fun mapRow(row: ResultRow) =
        PasskeyChallenge(
            id = row[PasskeyChallengesTable.id].value,
            userId = row[PasskeyChallengesTable.userId]?.value,
            type = PasskeyChallengeType.valueOf(row[PasskeyChallengesTable.type]),
            challenge = row[PasskeyChallengesTable.challenge],
            requestJson = row[PasskeyChallengesTable.requestJson],
            expiresAt = row[PasskeyChallengesTable.expiresAt],
            consumedAt = row[PasskeyChallengesTable.consumedAt],
            createdAt = row[PasskeyChallengesTable.createdAt],
        )
}
