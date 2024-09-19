package io.pcast.model


import com.fasterxml.uuid.Generators
import io.pcast.serializer.LocalDateTimeSerializer
import io.pcast.serializer.UuidSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Feed(
    @Serializable(with = UuidSerializer::class)
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),

    val title: String,

    val url: String,

    @Serializable(with = LocalDateTimeSerializer::class)
    val synchronizedAt: LocalDateTime? = null
)
