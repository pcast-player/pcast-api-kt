package io.pcast.module.auth.model

import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import io.pcast.helpers.generateUuidV7
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.util.UUID

private const val CREDENTIAL_ID_MAX_LENGTH = 1024
private const val TRANSPORTS_MAX_LENGTH = 255
private const val NICKNAME_MAX_LENGTH = 255

object PasskeyCredentialsTable : UUIDTable("passkey_credentials") {
    val userId = reference("user_id", UsersTable)
    val credentialId = varchar("credential_id", CREDENTIAL_ID_MAX_LENGTH).uniqueIndex()
    val publicKeyCose = text("public_key_cose")
    val signatureCount = long("signature_count").default(0)
    val transports = varchar("transports", TRANSPORTS_MAX_LENGTH).nullable()
    val nickname = varchar("nickname", NICKNAME_MAX_LENGTH).nullable()
    val backupEligible = bool("backup_eligible").nullable()
    val backedUp = bool("backed_up").nullable()
    val createdAt = datetime("created_at")
    val lastUsedAt = datetime("last_used_at").nullable()
}

@Single
class PasskeyCredentialRepository(
    private val db: Database,
) {
    fun create(
        userId: UUID,
        credentialId: String,
        publicKeyCose: String,
        signatureCount: Long,
        transports: String?,
        nickname: String?,
        backupEligible: Boolean?,
        backedUp: Boolean?,
    ): PasskeyCredential {
        val credential =
            PasskeyCredential(
                id = generateUuidV7(),
                userId = userId,
                credentialId = credentialId,
                publicKeyCose = publicKeyCose,
                signatureCount = signatureCount,
                transports = transports,
                nickname = nickname,
                backupEligible = backupEligible,
                backedUp = backedUp,
                createdAt = LocalDateTime.now(),
                lastUsedAt = null,
            )

        transaction(db) {
            PasskeyCredentialsTable.insert {
                it[id] = credential.id
                it[PasskeyCredentialsTable.userId] = credential.userId
                it[PasskeyCredentialsTable.credentialId] = credential.credentialId
                it[PasskeyCredentialsTable.publicKeyCose] = credential.publicKeyCose
                it[PasskeyCredentialsTable.signatureCount] = credential.signatureCount
                it[PasskeyCredentialsTable.transports] = credential.transports
                it[PasskeyCredentialsTable.nickname] = credential.nickname
                it[PasskeyCredentialsTable.backupEligible] = credential.backupEligible
                it[PasskeyCredentialsTable.backedUp] = credential.backedUp
                it[createdAt] = credential.createdAt
                it[lastUsedAt] = credential.lastUsedAt
            }
        }

        return credential
    }

    fun listByUser(userId: UUID): List<PasskeyCredential> =
        transaction(db) {
            PasskeyCredentialsTable
                .selectAll()
                .where { PasskeyCredentialsTable.userId eq userId }
                .map(::mapRow)
        }

    fun findByCredentialId(credentialId: String): PasskeyCredential? =
        transaction(db) {
            PasskeyCredentialsTable
                .selectAll()
                .where { PasskeyCredentialsTable.credentialId eq credentialId }
                .map(::mapRow)
                .singleOrNull()
        }

    fun findRegisteredByCredentialId(credentialId: ByteArray): Set<RegisteredCredential> =
        findByCredentialId(credentialId.base64Url)
            ?.let { setOf(it.toRegisteredCredential()) }
            ?: emptySet()

    fun findRegisteredByCredentialIdAndUserHandle(
        credentialId: ByteArray,
        userHandle: ByteArray,
    ): RegisteredCredential? =
        transaction(db) {
            PasskeyCredentialsTable
                .innerJoin(UsersTable)
                .selectAll()
                .where {
                    (PasskeyCredentialsTable.credentialId eq credentialId.base64Url) and
                        (UsersTable.passkeyUserHandle eq userHandle.base64Url)
                }.map(::mapRow)
                .singleOrNull()
                ?.toRegisteredCredential(userHandle)
        }

    fun findDescriptorsByUser(userId: UUID): Set<PublicKeyCredentialDescriptor> =
        listByUser(userId)
            .map { credential ->
                PublicKeyCredentialDescriptor
                    .builder()
                    .id(ByteArray.fromBase64Url(credential.credentialId))
                    .build()
            }.toSet()

    fun updateAfterAuthentication(
        credentialId: String,
        signatureCount: Long,
        backupEligible: Boolean?,
        backedUp: Boolean?,
    ): Boolean =
        transaction(db) {
            PasskeyCredentialsTable.update({ PasskeyCredentialsTable.credentialId eq credentialId }) {
                it[PasskeyCredentialsTable.signatureCount] = signatureCount
                it[PasskeyCredentialsTable.backupEligible] = backupEligible
                it[PasskeyCredentialsTable.backedUp] = backedUp
                it[lastUsedAt] = LocalDateTime.now()
            } > 0
        }

    fun deleteForUser(
        userId: UUID,
        credentialId: UUID,
    ): Boolean =
        transaction(db) {
            PasskeyCredentialsTable.deleteWhere {
                (PasskeyCredentialsTable.id eq credentialId) and (PasskeyCredentialsTable.userId eq userId)
            } > 0
        }

    private fun PasskeyCredential.toRegisteredCredential(userHandle: ByteArray? = null): RegisteredCredential =
        RegisteredCredential
            .builder()
            .credentialId(ByteArray.fromBase64Url(credentialId))
            .userHandle(userHandle ?: userHandleForUser(userId))
            .publicKeyCose(ByteArray.fromBase64Url(publicKeyCose))
            .signatureCount(signatureCount)
            .build()

    private fun userHandleForUser(userId: UUID): ByteArray =
        transaction(db) {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq userId }
                .single()
                .let { ByteArray.fromBase64Url(it[UsersTable.passkeyUserHandle]) }
        }

    private fun mapRow(row: ResultRow) =
        PasskeyCredential(
            id = row[PasskeyCredentialsTable.id].value,
            userId = row[PasskeyCredentialsTable.userId].value,
            credentialId = row[PasskeyCredentialsTable.credentialId],
            publicKeyCose = row[PasskeyCredentialsTable.publicKeyCose],
            signatureCount = row[PasskeyCredentialsTable.signatureCount],
            transports = row[PasskeyCredentialsTable.transports],
            nickname = row[PasskeyCredentialsTable.nickname],
            backupEligible = row[PasskeyCredentialsTable.backupEligible],
            backedUp = row[PasskeyCredentialsTable.backedUp],
            createdAt = row[PasskeyCredentialsTable.createdAt],
            lastUsedAt = row[PasskeyCredentialsTable.lastUsedAt],
        )
}
