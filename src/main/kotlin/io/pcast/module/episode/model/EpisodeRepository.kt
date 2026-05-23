package io.pcast.module.episode.model

import io.pcast.module.feed.model.FeedsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.util.UUID

private const val GUID_MAX_LENGTH = 1024
private const val TITLE_MAX_LENGTH = 500
private const val MEDIA_TYPE_MAX_LENGTH = 255

object EpisodesTable : UUIDTable("episodes") {
    val feedId = reference("feed_id", FeedsTable)
    val guid = varchar("guid", GUID_MAX_LENGTH)
    val title = varchar("title", TITLE_MAX_LENGTH)
    val description = text("description").nullable()
    val mediaUrl = text("media_url")
    val mediaType = varchar("media_type", MEDIA_TYPE_MAX_LENGTH).nullable()
    val durationSeconds = long("duration_seconds").nullable()
    val publishedAt = datetime("published_at").nullable()
    val imageUrl = text("image_url").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object EpisodeProgressTable : Table("episode_progress") {
    val userId = reference("user_id", io.pcast.module.auth.model.UsersTable)
    val episodeId = reference("episode_id", EpisodesTable)
    val positionSeconds = long("position_seconds")
    val durationSeconds = long("duration_seconds").nullable()
    val completed = bool("completed").default(false)
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(userId, episodeId)
}

@Single
class EpisodeRepository(
    private val db: Database,
) {
    fun upsert(episode: Episode): Episode {
        transaction(db) {
            EpisodesTable.upsert(EpisodesTable.feedId, EpisodesTable.guid) {
                it[id] = episode.id
                it[feedId] = episode.feedId
                it[guid] = episode.guid
                it[title] = episode.title
                it[description] = episode.description
                it[mediaUrl] = episode.mediaUrl
                it[mediaType] = episode.mediaType
                it[durationSeconds] = episode.durationSeconds
                it[publishedAt] = episode.publishedAt
                it[imageUrl] = episode.imageUrl
                it[createdAt] = episode.createdAt
                it[updatedAt] = episode.updatedAt
            }
        }
        return episode
    }

    fun listForUser(
        userId: UUID,
        feedNanoId: String?,
        limit: Int,
        before: LocalDateTime?,
    ): List<UserEpisode> =
        transaction(db) {
            val feedRows =
                FeedsTable
                    .selectAll()
                    .where {
                        if (feedNanoId == null) {
                            FeedsTable.userId eq userId
                        } else {
                            (FeedsTable.userId eq userId) and (FeedsTable.nanoId eq feedNanoId)
                        }
                    }.associateBy { it[FeedsTable.id].value }

            if (feedRows.isEmpty()) {
                emptyList()
            } else {
                val feedIds = feedRows.keys.toList()
                val episodes =
                    EpisodesTable
                        .selectAll()
                        .where {
                            val owned = EpisodesTable.feedId inList feedIds
                            if (before == null) {
                                owned
                            } else {
                                owned and
                                    (
                                        (EpisodesTable.publishedAt less before) or
                                            (
                                                EpisodesTable.publishedAt.isNull() and
                                                    (EpisodesTable.createdAt less before)
                                            )
                                    )
                            }
                        }.orderBy(
                            EpisodesTable.publishedAt to SortOrder.DESC_NULLS_LAST,
                            EpisodesTable.createdAt to SortOrder.DESC,
                        ).limit(limit)
                        .map(::mapEpisodeRow)

                val progressByEpisode = progressFor(userId, episodes.map { it.id })
                episodes.map { episode ->
                    val feed = feedRows.getValue(episode.feedId)
                    UserEpisode(
                        episode = episode,
                        feedNanoId = feed[FeedsTable.nanoId],
                        feedTitle = feed[FeedsTable.title],
                        progress = progressByEpisode[episode.id],
                    )
                }
            }
        }

    fun findForUser(
        episodeId: UUID,
        userId: UUID,
    ): UserEpisode? =
        transaction(db) {
            val row =
                EpisodesTable
                    .innerJoin(FeedsTable)
                    .selectAll()
                    .where {
                        (EpisodesTable.id eq episodeId) and (FeedsTable.userId eq userId)
                    }.singleOrNull()
                    ?: return@transaction null

            val episode = mapEpisodeRow(row)
            UserEpisode(
                episode = episode,
                feedNanoId = row[FeedsTable.nanoId],
                feedTitle = row[FeedsTable.title],
                progress = progressFor(userId, listOf(episode.id))[episode.id],
            )
        }

    fun upsertProgress(progress: EpisodeProgress) {
        transaction(db) {
            EpisodeProgressTable.upsert(EpisodeProgressTable.userId, EpisodeProgressTable.episodeId) {
                it[userId] = progress.userId
                it[episodeId] = progress.episodeId
                it[positionSeconds] = progress.positionSeconds
                it[durationSeconds] = progress.durationSeconds
                it[completed] = progress.completed
                it[updatedAt] = progress.updatedAt
            }
        }
    }

    private fun progressFor(
        userId: UUID,
        episodeIds: List<UUID>,
    ): Map<UUID, EpisodeProgress> {
        if (episodeIds.isEmpty()) return emptyMap()

        return EpisodeProgressTable
            .selectAll()
            .where {
                (EpisodeProgressTable.userId eq userId) and (EpisodeProgressTable.episodeId inList episodeIds)
            }.associate { row ->
                val progress =
                    EpisodeProgress(
                        userId = row[EpisodeProgressTable.userId].value,
                        episodeId = row[EpisodeProgressTable.episodeId].value,
                        positionSeconds = row[EpisodeProgressTable.positionSeconds],
                        durationSeconds = row[EpisodeProgressTable.durationSeconds],
                        completed = row[EpisodeProgressTable.completed],
                        updatedAt = row[EpisodeProgressTable.updatedAt],
                    )
                progress.episodeId to progress
            }
    }

    private fun mapEpisodeRow(row: ResultRow) =
        Episode(
            id = row[EpisodesTable.id].value,
            feedId = row[EpisodesTable.feedId].value,
            guid = row[EpisodesTable.guid],
            title = row[EpisodesTable.title],
            description = row[EpisodesTable.description],
            mediaUrl = row[EpisodesTable.mediaUrl],
            mediaType = row[EpisodesTable.mediaType],
            durationSeconds = row[EpisodesTable.durationSeconds],
            publishedAt = row[EpisodesTable.publishedAt],
            imageUrl = row[EpisodesTable.imageUrl],
            createdAt = row[EpisodesTable.createdAt],
            updatedAt = row[EpisodesTable.updatedAt],
        )
}
