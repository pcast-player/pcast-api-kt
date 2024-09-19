package io.pcast.model

import io.pcast.helpers.generateUuidV7
import io.pcast.result.Result
import java.time.LocalDateTime
import java.util.UUID

private fun createFakeFeed(i: Int) = Feed(
    id = generateUuidV7(),
    title = "Feed $i",
    url = "https://rss.pcast.io/news$i.rss",
    synchronizedAt = LocalDateTime.now().minusDays(i.toLong())
)

private val FEEDS = buildMap {
    for (i in 1..10) {
        createFakeFeed(i).also { put(it.id, it) }
    }
}.toMutableMap()

class FakeFeedRepository : FeedRepository {
    override fun save(feed: Feed) {
        FEEDS[feed.id] = feed
    }

    override fun findAll() = FEEDS.values.toList()

    override fun find(id: UUID): Result<Feed, Exception> {
        val feed = FEEDS[id]

        return if (feed != null) {
            Result.ok(feed)
        } else {
            Result.error(FeedNotFoundException())
        }
    }

    override fun delete(id: UUID) {
        FEEDS.remove(id)
    }
}