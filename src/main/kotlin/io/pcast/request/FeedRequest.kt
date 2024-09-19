package io.pcast.request

import io.pcast.helpers.generateUuidV7
import io.pcast.model.Feed
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FeedRequest(
    val title: String,

    val url: String
) {
    fun toFeed(id: UUID = generateUuidV7()) = Feed(
        id = id,
        title = title,
        url = url,
        synchronizedAt = null
    )
}
