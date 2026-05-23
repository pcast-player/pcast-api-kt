package io.pcast.module.episode.model

import java.time.LocalDateTime
import java.util.UUID

data class Episode(
    val id: UUID,
    val feedId: UUID,
    val guid: String,
    val title: String,
    val description: String?,
    val mediaUrl: String,
    val mediaType: String?,
    val durationSeconds: Long?,
    val publishedAt: LocalDateTime?,
    val imageUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class EpisodeProgress(
    val userId: UUID,
    val episodeId: UUID,
    val positionSeconds: Long,
    val durationSeconds: Long?,
    val completed: Boolean,
    val updatedAt: LocalDateTime,
)

data class UserEpisode(
    val episode: Episode,
    val feedNanoId: String,
    val feedTitle: String,
    val progress: EpisodeProgress?,
)
