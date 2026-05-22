package io.pcast.module.feed.model

import java.time.LocalDateTime
import java.util.UUID

data class Feed(
    val id: UUID,
    val userId: UUID,
    val nanoId: String,
    val title: String,
    val url: String,
    val synchronizedAt: LocalDateTime?,
)
