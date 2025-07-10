package io.pcast.controller.feed

import io.pcast.model.feed.Feed
import io.pcast.serializer.LocalDateTimeSerializer
import io.pcast.serializer.UuidSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class FeedResponse(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val nanoId: String,
    val title: String,
    val url: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val synchronizedAt: LocalDateTime? = null,
) {
    constructor(f: Feed) : this(
        id = f.id,
        nanoId = f.nanoId,
        title = f.title,
        url = f.url,
        synchronizedAt = f.synchronizedAt,
    )
}
