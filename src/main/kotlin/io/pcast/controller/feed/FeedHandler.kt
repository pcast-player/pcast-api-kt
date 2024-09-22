package io.pcast.controller.feed

import io.pcast.model.feed.Feed
import io.pcast.model.feed.FeedRepository
import io.pcast.result.Result
import java.util.UUID

class FeedHandler(
    private val repository: FeedRepository
) {
    fun getFeeds() = repository.findAll()

    fun getFeed(id: UUID) = repository.find(id)

    fun addFeed(request: FeedRequest): Result<Feed, Exception> {
        val feed = request.toFeed()

        repository.save(feed)

        return Result.ok(feed)
    }

    fun updateFeed(id: UUID, request: FeedRequest) {
        val feed = request.toFeed(id)

        repository.save(feed)
    }
}