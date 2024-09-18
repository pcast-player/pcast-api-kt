package io.pcast.model

import io.pcast.result.toResult
import java.time.LocalDateTime

private val FEEDS = mutableListOf(
    Feed(
        id = 1,
        title = "PCast News",
        url = "https://rss.pcast.io/news.rss",
        synchronizedAt = LocalDateTime.now().minusDays(1L)
    ),
    Feed(
        id = 2,
        title = "PCast News 2",
        url = "https://rss.pcast.io/news2.rss",
        synchronizedAt = LocalDateTime.now().minusDays(2L)
    ),
    Feed(
        id = 3,
        title = "PCast News 3",
        url = "https://rss.pcast.io/news3.rss",
        synchronizedAt = LocalDateTime.now().minusDays(3L)
    )
)

class FakeFeedRepository : FeedRepository {
    override fun save(feed: Feed) {
        val i = FEEDS.indexOf(feed)

        if (i != -1) {
            FEEDS[i] = feed
        } else FEEDS.add(feed)
    }

    override fun findAll() = FEEDS

    override fun find(id: Int) = toResult {
        FEEDS.find { it.id == id} ?: throw FeedNotFoundException()
    }

    override fun delete(id: Int) {
        FEEDS.removeIf { it.id == id }
    }
}