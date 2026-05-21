package io.pcast.module.feed.request

import io.pcast.helpers.generateNanoId
import io.pcast.helpers.generateUuidV7
import io.pcast.module.feed.model.Feed
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FeedRequest(
    val title: String,
    val url: String,
) {
    fun toModel(
        userId: UUID,
        id: UUID = generateUuidV7(),
        nanoId: String = generateNanoId(),
    ) = Feed(
        id = id,
        userId = userId,
        nanoId = nanoId,
        title = title,
        url = url,
        synchronizedAt = null,
    )
}
