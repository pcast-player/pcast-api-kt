package io.pcast.model.feed

import io.pcast.result.Result
import java.util.UUID

interface FeedRepository {
    fun save(feed: Feed): Result<Unit, Exception>
    fun findAll(): Result<List<Feed>, Exception>
    fun find(id: UUID): Result<Feed, Exception>
    fun delete(id: UUID): Result<Unit, Exception>
}