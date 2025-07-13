package io.pcast.service.feed

import io.pcast.helpers.generateNanoId
import io.pcast.helpers.generateUuidV7
import io.pcast.model.feed.Feed
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FeedRequest(
    val title: String,
    val url: String,
) {
    fun toModel(
        id: UUID = generateUuidV7(),
        nanoId: String = generateNanoId(),
    ) = Feed(
        id = id,
        nanoId = nanoId,
        title = title,
        url = url,
        synchronizedAt = null,
    )
}
