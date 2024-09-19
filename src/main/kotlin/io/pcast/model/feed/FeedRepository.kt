package io.pcast.model.feed

import io.pcast.result.Result
import java.util.UUID

interface FeedRepository {
    fun save(feed: Feed)
    fun findAll(): List<Feed>
    fun find(id: UUID): Result<Feed, Exception>
    fun delete(id: UUID)
}