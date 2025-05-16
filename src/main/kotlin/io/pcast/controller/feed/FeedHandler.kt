package io.pcast.controller.feed

import io.pcast.model.feed.Feed
import io.pcast.model.feed.FeedRepository
import io.pcast.result.Result

class FeedHandler(
    private val repository: FeedRepository
) {
    fun getFeeds() = repository.findAll()

    fun getFeed(nanoId: String) = repository.findByNanoId(nanoId)

    fun addFeed(request: FeedRequest): Result<Feed, Exception> {
        val feed = request.toFeed()

        repository.save(feed)

        return Result.ok(feed)
    }

    fun updateFeed(nanoId: String, request: FeedRequest) {
        val feed = request.toFeed(nanoId = nanoId)

        repository.save(feed)
    }
}