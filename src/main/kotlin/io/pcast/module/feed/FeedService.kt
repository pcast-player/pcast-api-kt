package io.pcast.module.feed

import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.module.feed.model.Feed
import io.pcast.module.feed.model.FeedRepository
import io.pcast.module.feed.opml.OpmlFile
import io.pcast.module.feed.request.FeedRequest
import org.koin.core.annotation.Single
import java.util.UUID

data class FeedPage(
    val feeds: List<Feed>,
    val total: Long,
)

@Single
class FeedService(
    private val repository: FeedRepository,
) {
    fun getFeeds(userId: UUID): List<Feed> = repository.findAll(userId)

    fun getFeeds(
        userId: UUID,
        page: Int,
        pageSize: Int,
    ): FeedPage {
        val offset = (page - 1) * pageSize
        return FeedPage(
            feeds = repository.findPage(userId, limit = pageSize, offset = offset),
            total = repository.count(userId),
        )
    }

    fun getFeed(
        nanoId: String,
        userId: UUID,
    ): Feed = repository.findByNanoId(nanoId, userId)

    fun addFeed(
        request: FeedRequest,
        userId: UUID,
    ): Feed {
        val feed = request.toModel(userId = userId)
        repository.create(feed)
        return feed
    }

    fun addFeeds(
        opmlFile: OpmlFile,
        userId: UUID,
    ): List<Feed> =
        buildList {
            for (outline in opmlFile.body.outlines) {
                val feed = outline.toFeed(userId = userId)
                repository.create(feed)
                add(feed)
            }
        }

    fun updateFeed(
        nanoId: String,
        request: FeedRequest,
        userId: UUID,
    ) {
        val feed = request.toModel(userId = userId, nanoId = nanoId)
        val updated = repository.update(feed, ownerId = userId)
        if (!updated) throw AbortError(HttpError.NotFound, "No feed found")
    }

    fun deleteFeed(
        nanoId: String,
        userId: UUID,
    ) {
        val deleted = repository.deleteByNanoId(nanoId, ownerId = userId)
        if (!deleted) throw AbortError(HttpError.NotFound, "No feed found")
    }
}
