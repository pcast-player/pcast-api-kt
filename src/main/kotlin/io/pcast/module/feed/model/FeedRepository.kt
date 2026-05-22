package io.pcast.module.feed.model

import io.pcast.helpers.NANO_ID_LENGTH
import io.pcast.module.feed.error.FeedNotFoundError
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
import java.util.UUID

private const val VARCHAR_MAX_LENGTH = 255

object FeedsTable : UUIDTable("feeds") {
    val userId = reference("user_id", io.pcast.module.auth.model.UsersTable)
    val nanoId = char("nano_id", NANO_ID_LENGTH)
    val title = varchar("title", VARCHAR_MAX_LENGTH)
    val url = varchar("url", VARCHAR_MAX_LENGTH)
    val synchronizedAt = datetime("synchronized_at").nullable()
}

@Single
class FeedRepository(
    private val db: Database,
) {
    fun create(feed: Feed) {
        transaction(db) {
            FeedsTable.insert {
                it[id] = feed.id
                it[userId] = feed.userId
                it[nanoId] = feed.nanoId
                it[title] = feed.title
                it[url] = feed.url
                it[synchronizedAt] = feed.synchronizedAt
            }
        }
    }

    /**
     * Updates a feed owned by [ownerId]. Returns true if a row was updated,
     * false (caller should surface as 404) if the nanoId is not owned by this user.
     */
    fun update(
        feed: Feed,
        ownerId: UUID,
    ): Boolean =
        transaction(db) {
            FeedsTable.update({
                (FeedsTable.nanoId eq feed.nanoId) and (FeedsTable.userId eq ownerId)
            }) {
                it[title] = feed.title
                it[url] = feed.url
                it[synchronizedAt] = feed.synchronizedAt
            } > 0
        }

    fun findAll(ownerId: UUID): List<Feed> =
        transaction(db) {
            FeedsTable
                .selectAll()
                .where { FeedsTable.userId eq ownerId }
                .map(::mapRow)
        }

    fun find(
        id: UUID,
        ownerId: UUID,
    ): Feed =
        transaction(db) {
            FeedsTable
                .selectAll()
                .where { (FeedsTable.id eq id) and (FeedsTable.userId eq ownerId) }
                .map(::mapRow)
                .singleOrNull()
        } ?: throw FeedNotFoundError()

    fun findByNanoId(
        nanoId: String,
        ownerId: UUID,
    ): Feed =
        transaction(db) {
            FeedsTable
                .selectAll()
                .where { (FeedsTable.nanoId eq nanoId) and (FeedsTable.userId eq ownerId) }
                .map(::mapRow)
                .singleOrNull()
        } ?: throw FeedNotFoundError()

    fun delete(
        id: UUID,
        ownerId: UUID,
    ) {
        transaction(db) {
            FeedsTable.deleteWhere { (FeedsTable.id eq id) and (FeedsTable.userId eq ownerId) }
        }
    }

    private fun mapRow(row: ResultRow) =
        Feed(
            id = row[FeedsTable.id].value,
            userId = row[FeedsTable.userId].value,
            nanoId = row[FeedsTable.nanoId],
            title = row[FeedsTable.title],
            url = row[FeedsTable.url],
            synchronizedAt = row[FeedsTable.synchronizedAt],
        )
}
