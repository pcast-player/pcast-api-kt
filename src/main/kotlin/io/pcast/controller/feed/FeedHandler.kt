package io.pcast.controller.feed

import io.pcast.model.feed.FeedRepository
import io.pcast.result.Result
import io.pcast.result.isOk
import java.util.UUID

class FeedHandler(
    private val repository: FeedRepository
) {
    fun getFeeds() = repository
        .findAll()
        .map(::FeedResponse)

    fun getFeed(id: UUID): Result<FeedResponse, Exception> {
        val result = repository.find(id)

        return if (result.isOk()) {
            Result.ok(FeedResponse(result.value))
        } else {
            Result.error(result.error)
        }
    }

    fun addFeed(request: FeedRequest): FeedResponse {
        val feed = request.toFeed()

        repository.save(feed)

        return FeedResponse(feed)
    }

    fun updateFeed(id: UUID, request: FeedRequest) {
        val feed = request.toFeed(id)

        repository.save(feed)
    }
}