package io.pcast.model

import io.pcast.result.Result

interface FeedRepository {
    fun save(feed: Feed)
    fun findAll(): List<Feed>
    fun find(id: Int): Result<Feed, Exception>
    fun delete(id: Int)
}