package io.pcast.module.episode.request

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeProgressRequest(
    val positionSeconds: Long,
    val durationSeconds: Long?,
    val completed: Boolean,
)
