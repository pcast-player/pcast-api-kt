package io.pcast.model


import io.pcast.serializer.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Feed(
    val id: Int,
    val title: String,
    val url: String,

    @Serializable(with = LocalDateTimeSerializer::class)
    val synchronizedAt: LocalDateTime? = null
)
