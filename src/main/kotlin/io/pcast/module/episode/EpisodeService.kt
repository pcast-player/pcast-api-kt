package io.pcast.module.episode

import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.module.episode.model.EpisodeProgress
import io.pcast.module.episode.model.EpisodeRepository
import io.pcast.module.episode.model.UserEpisode
import io.pcast.module.episode.request.EpisodeProgressRequest
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.util.UUID

private const val DEFAULT_LIMIT = 50
private const val MAX_LIMIT = 100
private const val DRIFT_ALLOWANCE_SECONDS = 5

@Single
class EpisodeService(
    private val repository: EpisodeRepository,
) {
    fun listEpisodes(
        userId: UUID,
        feedNanoId: String?,
        limit: Int?,
        before: LocalDateTime?,
    ): List<UserEpisode> {
        val resolvedLimit = limit ?: DEFAULT_LIMIT
        if (resolvedLimit <= 0) throw AbortError(HttpError.BadRequest, "limit must be positive")
        if (resolvedLimit > MAX_LIMIT) throw AbortError(HttpError.BadRequest, "limit must be at most $MAX_LIMIT")

        return repository.listForUser(userId, feedNanoId, resolvedLimit, before)
    }

    fun getEpisode(
        episodeId: UUID,
        userId: UUID,
    ): UserEpisode =
        repository.findForUser(episodeId, userId)
            ?: throw AbortError(HttpError.NotFound, "Episode not found")

    fun updateProgress(
        episodeId: UUID,
        userId: UUID,
        request: EpisodeProgressRequest,
    ) {
        val episode = getEpisode(episodeId, userId).episode
        validateProgress(request, episode.durationSeconds)
        repository.upsertProgress(
            EpisodeProgress(
                userId = userId,
                episodeId = episodeId,
                positionSeconds = request.positionSeconds,
                durationSeconds = request.durationSeconds,
                completed = request.completed,
                updatedAt = LocalDateTime.now(),
            ),
        )
    }

    private fun validateProgress(
        request: EpisodeProgressRequest,
        storedDurationSeconds: Long?,
    ) {
        if (request.positionSeconds < 0) {
            throw AbortError(HttpError.BadRequest, "positionSeconds must be greater than or equal to 0")
        }
        if (request.durationSeconds != null && request.durationSeconds <= 0) {
            throw AbortError(HttpError.BadRequest, "durationSeconds must be greater than 0")
        }

        val effectiveDuration = request.durationSeconds ?: storedDurationSeconds
        if (request.completed && effectiveDuration == null) {
            throw AbortError(HttpError.BadRequest, "durationSeconds is required when completed is true")
        }
        if (request.durationSeconds != null &&
            request.positionSeconds > request.durationSeconds + DRIFT_ALLOWANCE_SECONDS
        ) {
            throw AbortError(HttpError.BadRequest, "positionSeconds must be less than or equal to durationSeconds")
        }
    }
}
