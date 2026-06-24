package io.pcast.module.feed

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.helpers.generateUuidV7
import io.pcast.module.episode.model.Episode
import io.pcast.module.episode.model.EpisodeRepository
import io.pcast.module.feed.model.Feed
import io.pcast.module.feed.model.FeedRepository
import io.pcast.module.feed.opml.OpmlFile
import io.pcast.module.feed.request.FeedRequest
import org.koin.core.annotation.Single
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Single
class FeedService(
    private val repository: FeedRepository,
    private val episodeRepository: EpisodeRepository,
) {
    fun getFeeds(userId: UUID): List<Feed> = repository.findAll(userId)

    fun getFeed(
        nanoId: String,
        userId: UUID,
    ): Feed = repository.findByNanoId(nanoId, userId)

    fun addFeed(
        request: FeedRequest,
        userId: UUID,
    ): Feed {
        val feed = request.toModel(userId = userId)
        repository.create(feed)
        return feed
    }

    fun addFeeds(
        opmlFile: OpmlFile,
        userId: UUID,
    ): List<Feed> =
        buildList {
            for (outline in opmlFile.body.outlines) {
                val feed = outline.toFeed(userId = userId)
                repository.create(feed)
                add(feed)
            }
        }

    fun updateFeed(
        nanoId: String,
        request: FeedRequest,
        userId: UUID,
    ) {
        val feed = request.toModel(userId = userId, nanoId = nanoId)
        val updated = repository.update(feed, ownerId = userId)
        if (!updated) throw AbortError(HttpError.NotFound, "No feed found")
    }

    fun deleteFeed(
        nanoId: String,
        userId: UUID,
    ) {
        val deleted = repository.deleteByNanoId(nanoId, ownerId = userId)
        if (!deleted) throw AbortError(HttpError.NotFound, "Feed not found")
    }

    fun syncFeed(
        nanoId: String,
        userId: UUID,
    ): FeedSyncResult {
        val feed = getFeed(nanoId, userId)
        val uri = validateFeedUri(feed.url)
        val entries = fetchEntries(uri)

        var created = 0
        var updated = 0
        var skipped = 0
        val now = LocalDateTime.now()

        for (entry in entries) {
            val episode = entry.toEpisode(feed.id, now)
            if (episode == null) {
                skipped += 1
            } else if (episodeRepository.upsert(episode)) {
                created += 1
            } else {
                updated += 1
            }
        }

        if (created + updated == 0) {
            throw AbortError(HttpError.BadRequest, "Invalid podcast feed")
        }

        return FeedSyncResult(
            feed = repository.updateSynchronizedAt(feed, now),
            createdCount = created,
            updatedCount = updated,
            skippedCount = skipped,
        )
    }

    private fun validateFeedUri(url: String): URI {
        val uri =
            runCatching { URI(url) }
                .getOrElse { throw AbortError(HttpError.BadRequest, "Feed URL must use http or https") }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw AbortError(HttpError.BadRequest, "Feed URL must use http or https")
        }

        val host = uri.host ?: throw AbortError(HttpError.BadRequest, "Feed URL host is not allowed")
        val addresses =
            runCatching { InetAddress.getAllByName(host) }
                .getOrElse { throw AbortError(HttpError.BadRequest, "Feed URL host is not allowed", it) }
        if (addresses.any {
                it.isAnyLocalAddress ||
                    it.isLoopbackAddress ||
                    it.isLinkLocalAddress ||
                    it.isSiteLocalAddress ||
                    it.isMulticastAddress
            }
        ) {
            throw AbortError(HttpError.BadRequest, "Feed URL host is not allowed")
        }

        return uri
    }

    private fun fetchEntries(uri: URI): List<SyndEntry> =
        runCatching {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.inputStream.use { stream ->
                SyndFeedInput().build(XmlReader(stream)).entries
            }
        }.getOrElse { throw AbortError(HttpError.BadRequest, "Invalid podcast feed", it) }

    private fun SyndEntry.toEpisode(
        feedId: UUID,
        now: LocalDateTime,
    ): Episode? {
        val media = enclosures.firstOrNull { !it.url.isNullOrBlank() } ?: return null
        val title = title?.takeIf { it.isNotBlank() } ?: return null
        val guid = uri?.takeIf { it.isNotBlank() } ?: link?.takeIf { it.isNotBlank() } ?: media.url
        val published = publishedDate ?: updatedDate

        return Episode(
            id = generateUuidV7(),
            feedId = feedId,
            guid = guid.take(1024),
            title = title.take(500),
            description = description?.value,
            mediaUrl = media.url,
            mediaType = media.type,
            durationSeconds = null,
            publishedAt = published?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime(),
            imageUrl = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}

data class FeedSyncResult(
    val feed: Feed,
    val createdCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
)
