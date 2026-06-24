package io.pcast.module.feed.response

import io.pcast.module.feed.FeedSyncResult
import kotlinx.serialization.Serializable

@Serializable
data class FeedSyncResponse(
    val feed: FeedResponse,
    val createdCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
) {
    constructor(result: FeedSyncResult) : this(
        feed = FeedResponse(result.feed),
        createdCount = result.createdCount,
        updatedCount = result.updatedCount,
        skippedCount = result.skippedCount,
    )
}
