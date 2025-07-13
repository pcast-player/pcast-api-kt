package io.pcast.service.feed

import io.pcast.model.feed.Feed
import io.pcast.model.feed.FeedRepository
import io.pcast.result.Result
import io.pcast.service.feed.opml.OpmlFile

class FeedService(
    private val repository: FeedRepository,
) {
    fun getFeeds() = repository.findAll()

    fun getFeed(nanoId: String) = repository.findByNanoId(nanoId)

    fun addFeed(request: FeedRequest): Result<Feed, Exception> {
        val feed = request.toModel()

        repository.save(feed)

        return Result.ok(feed)
    }

    fun addFeeds(opmlFile: OpmlFile): Result<List<Feed>, Exception> {
        val feeds =
            buildList {
                for (outline in opmlFile.body.outlines) {
                    val feed = outline.toFeed()

                    repository.save(feed).also { add(feed) }
                }
            }

        return Result.ok(feeds)
    }

    fun updateFeed(
        nanoId: String,
        request: FeedRequest,
    ) {
        val feed = request.toModel(nanoId = nanoId)

        repository.save(feed)
    }
}
