package io.pcast.model.feed

import io.pcast.result.Result
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
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

class RealFeedRepository : FeedRepository {
    init {
        Database.connect("jdbc:sqlite:feeds.db", "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(FeedsTable)
        }
    }

    override fun save(feed: Feed): Result<Unit, Exception> {
        return try {
            transaction {
                // Check if the feed already exists
                val existingFeed = FeedsTable
                    .selectAll()
                    .where { FeedsTable.id eq feed.id }
                    .singleOrNull()
                
                if (existingFeed != null) {
                    // Update existing feed
                    FeedsTable.update({ FeedsTable.id eq feed.id }) {
                        it[title] = feed.title
                        it[url] = feed.url
                        it[synchronizedAt] = feed.synchronizedAt
                    }
                } else {
                    // Insert new feed
                    FeedsTable.insert {
                        it[id] = feed.id
                        it[title] = feed.title
                        it[url] = feed.url
                        it[synchronizedAt] = feed.synchronizedAt
                    }
                }
            }

            Result.ok()
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override fun findAll(): Result<List<Feed>, Exception> {
        return try {
            val feeds = transaction {
                FeedsTable.selectAll().map { row ->
                    Feed(
                        id = row[FeedsTable.id].value,
                        title = row[FeedsTable.title],
                        url = row[FeedsTable.url],
                        synchronizedAt = row[FeedsTable.synchronizedAt]
                    )
                }
            }

            Result.ok(feeds)
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override fun find(id: UUID): Result<Feed, Exception> {
        return try {
            val feed = transaction {
                FeedsTable
                    .selectAll()
                    .where { FeedsTable.id eq id }
                    .map { row ->
                        Feed(
                            id = row[FeedsTable.id].value,
                            title = row[FeedsTable.title],
                            url = row[FeedsTable.url],
                            synchronizedAt = row[FeedsTable.synchronizedAt]
                        )
                    }
                    .singleOrNull()
            }

            if (feed != null) {
                Result.ok(feed)
            } else {
                Result.error(FeedNotFoundException())
            }
        } catch (e: Exception) {
            Result.error(e)
        }
    }

    override fun delete(id: UUID): Result<Unit, Exception> {
        return try {
            transaction {
                FeedsTable.deleteWhere { FeedsTable.id eq id }
            }

            Result.ok()
        } catch (e: Exception) {
            Result.error(e)
        }
    }
}