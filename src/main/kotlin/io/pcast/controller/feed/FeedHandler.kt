package io.pcast.controller.feed

import io.pcast.controller.feed.opml.OpmlFile
import io.pcast.model.feed.Feed
import io.pcast.model.feed.FeedRepository
import io.pcast.result.Result

class FeedHandler(
    private val repository: FeedRepository,
) {
    fun getFeeds() = repository.findAll()

    fun getFeed(nanoId: String) = repository.findByNanoId(nanoId)

    fun addFeed(request: FeedRequest): Result<Feed, Exception> {
        val feed = request.toFeed()

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
        val feed = request.toFeed(nanoId = nanoId)

        repository.save(feed)
    }
}
