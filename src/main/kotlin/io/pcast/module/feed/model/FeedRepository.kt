package io.pcast.module.feed.model

import io.pcast.helpers.NANO_ID_LENGTH
import io.pcast.module.feed.error.FeedNotFoundError
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

private const val VARCHAR_MAX_LENGTH = 255

object FeedsTable : UUIDTable("feeds") {
    val nanoId = char("nano_id", NANO_ID_LENGTH)
    val title = varchar("title", VARCHAR_MAX_LENGTH)
    val url = varchar("url", VARCHAR_MAX_LENGTH)
    val synchronizedAt = datetime("synchronized_at").nullable()
}

class FeedRepository(
    private val db: Database,
) {
    fun save(feed: Feed) {
        transaction(db) {
            val existingFeed =
                FeedsTable
                    .selectAll()
                    .where { FeedsTable.nanoId eq feed.nanoId }
                    .singleOrNull()

            if (existingFeed != null) {
                update(feed)
            } else {
                insert(feed)
            }
        }
    }

    private fun update(feed: Feed) {
        FeedsTable.update({ FeedsTable.nanoId eq feed.nanoId }) {
            it[title] = feed.title
            it[url] = feed.url
            it[synchronizedAt] = feed.synchronizedAt
        }
    }

    private fun insert(feed: Feed) {
        FeedsTable.insert {
            it[id] = feed.id
            it[nanoId] = feed.nanoId
            it[title] = feed.title
            it[url] = feed.url
            it[synchronizedAt] = feed.synchronizedAt
        }
    }

    fun findAll() =
        transaction(db) {
            FeedsTable.selectAll().map(::mapRow)
        }

    fun find(id: UUID) =
        transaction(db) {
            FeedsTable
                .selectAll()
                .where { FeedsTable.id eq id }
                .map(::mapRow)
                .singleOrNull()
        } ?: throw FeedNotFoundError()

    fun findByNanoId(nanoId: String) =
        transaction(db) {
            FeedsTable
                .selectAll()
                .where { FeedsTable.nanoId eq nanoId }
                .map(::mapRow)
                .singleOrNull()
        } ?: throw FeedNotFoundError()

    fun delete(id: UUID) {
        transaction(db) {
            FeedsTable.deleteWhere { FeedsTable.id eq id }
        }
    }

    private fun mapRow(row: ResultRow) =
        Feed(
            id = row[FeedsTable.id].value,
            nanoId = row[FeedsTable.nanoId],
            title = row[FeedsTable.title],
            url = row[FeedsTable.url],
            synchronizedAt = row[FeedsTable.synchronizedAt],
        )
}
