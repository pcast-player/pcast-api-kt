package io.pcast.model


import java.time.LocalDateTime
import java.util.UUID

data class Feed(
    val id: UUID,
    val title: String,
    val url: String,
    val synchronizedAt: LocalDateTime?
)
