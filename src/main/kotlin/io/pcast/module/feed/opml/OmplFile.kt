package io.pcast.module.feed.opml

import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.helpers.generateNanoId
import io.pcast.helpers.generateUuidV7
import io.pcast.module.feed.model.Feed
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.util.UUID

@Serializable
@XmlSerialName("opml")
data class OpmlFile(
    val version: String,
    val head: OpmlHead,
    val body: OpmlBody,
)

@Serializable
@XmlSerialName("head")
data class OpmlHead(
    @XmlElement
    val title: String,
)

@Serializable
@XmlSerialName("body")
data class OpmlBody(
    @XmlSerialName("outline")
    val outlines: OpmlOutlines,
)

@Serializable
@XmlSerialName("outline")
data class OpmlOutlines(
    val text: String,
    val outlines: List<OpmlOutline>,
) : Iterable<OpmlOutline> {
    override fun iterator() = outlines.iterator()
}

@Serializable
@XmlSerialName("outline")
data class OpmlOutline(
    val text: String,
    val type: String,
    val xmlUrl: String,
) {
    fun toFeed(
        userId: UUID,
        id: UUID = generateUuidV7(),
        nanoId: String = generateNanoId(),
    ): Feed {
        // Reject non-http(s) URL schemes to prevent javascript:/data: injection
        val scheme = xmlUrl.substringBefore("://").lowercase()
        if (scheme != "http" && scheme != "https") {
            throw AbortError(HttpError.BadRequest, "OPML feed URL must use http or https scheme")
        }

        return Feed(
            id = id,
            userId = userId,
            nanoId = nanoId,
            title = text,
            url = xmlUrl,
            synchronizedAt = null,
        )
    }
}
