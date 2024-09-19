package io.pcast.controller.feed

import io.pcast.helpers.generateUuidV7
import io.pcast.model.feed.Feed
import io.pcast.serializer.LocalDateTimeSerializer
import io.pcast.serializer.UuidSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class FeedResponse(
    @Serializable(with = UuidSerializer::class)
    val id: UUID = generateUuidV7(),

    val title: String,

    val url: String,

    @Serializable(with = LocalDateTimeSerializer::class)
    val synchronizedAt: LocalDateTime? = null
) {
    constructor(f: Feed) : this(
        id = f.id,
        title = f.title,
        url = f.url,
        synchronizedAt = f.synchronizedAt
    )
}

