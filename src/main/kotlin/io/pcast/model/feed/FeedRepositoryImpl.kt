package io.pcast.model.feed

import io.pcast.result.attempt
import io.pcast.result.attemptEmpty
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

object FeedsTable : UUIDTable("feeds") {
    val title = varchar("title", 255)
    val url = varchar("url", 255)
    val synchronizedAt = datetime("synchronized_at").nullable()
}

class FeedRepositoryImpl(
    private val db: Database
) : FeedRepository {
    override fun save(feed: Feed) = attempt {
        transaction(db) {
            val existingFeed = FeedsTable
                .selectAll()
                .where { FeedsTable.id eq feed.id }
                .singleOrNull()

            if (existingFeed != null) {
                update(feed)
            } else {
                insert(feed)
            }
        }
    }

    private fun update(feed: Feed) {
        FeedsTable.update({ FeedsTable.id eq feed.id }) {
            it[title] = feed.title
            it[url] = feed.url
            it[synchronizedAt] = feed.synchronizedAt
        }
    }

    private fun insert(feed: Feed) {
        FeedsTable.insert {
            it[id] = feed.id
            it[title] = feed.title
            it[url] = feed.url
            it[synchronizedAt] = feed.synchronizedAt
        }
    }

    override fun findAll() = attempt {
         transaction(db) {
            FeedsTable.selectAll().map(::mapRow)
        }
    }

    override fun find(
        id: UUID
    ) = attempt {
        transaction(db) {
            FeedsTable
                .selectAll()
                .where { FeedsTable.id eq id }
                .map(::mapRow)
                .singleOrNull()
        } ?: throw FeedNotFoundException()
    }

    override fun delete(
        id: UUID
    ) = attemptEmpty {
        transaction(db) {
            FeedsTable.deleteWhere { FeedsTable.id eq id }
        }
    }

    private fun mapRow(row: ResultRow) = Feed(
        id = row[FeedsTable.id].value,
        title = row[FeedsTable.title],
        url = row[FeedsTable.url],
        synchronizedAt = row[FeedsTable.synchronizedAt]
    )
}