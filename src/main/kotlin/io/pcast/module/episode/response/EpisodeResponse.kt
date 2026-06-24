package io.pcast.module.episode.response

import io.pcast.module.episode.model.EpisodeProgress
import io.pcast.module.episode.model.UserEpisode
import io.pcast.serializer.LocalDateTimeSerializer
import io.pcast.serializer.UuidSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class EpisodeProgressResponse(
    val positionSeconds: Long,
    val durationSeconds: Long?,
    val completed: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
) {
    constructor(progress: EpisodeProgress) : this(
        positionSeconds = progress.positionSeconds,
        durationSeconds = progress.durationSeconds,
        completed = progress.completed,
        updatedAt = progress.updatedAt,
    )
}

@Serializable
data class EpisodeResponse(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val feedNanoId: String,
    val feedTitle: String,
    val guid: String,
    val title: String,
    val description: String?,
    val mediaUrl: String,
    val mediaType: String?,
    val durationSeconds: Long?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val publishedAt: LocalDateTime?,
    val imageUrl: String?,
    val progress: EpisodeProgressResponse?,
) {
    constructor(userEpisode: UserEpisode) : this(
        id = userEpisode.episode.id,
        feedNanoId = userEpisode.feedNanoId,
        feedTitle = userEpisode.feedTitle,
        guid = userEpisode.episode.guid,
        title = userEpisode.episode.title,
        description = userEpisode.episode.description,
        mediaUrl = userEpisode.episode.mediaUrl,
        mediaType = userEpisode.episode.mediaType,
        durationSeconds = userEpisode.episode.durationSeconds,
        publishedAt = userEpisode.episode.publishedAt,
        imageUrl = userEpisode.episode.imageUrl,
        progress = userEpisode.progress?.let(::EpisodeProgressResponse),
    )
}
