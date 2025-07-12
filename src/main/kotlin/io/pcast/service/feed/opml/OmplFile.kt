
package io.pcast.service.feed.opml

import io.pcast.helpers.generateNanoId
import io.pcast.helpers.generateUuidV7
import io.pcast.model.feed.Feed
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
        id: UUID = generateUuidV7(),
        nanoId: String = generateNanoId(),
    ) = Feed(
        id = id,
        nanoId = nanoId,
        title = text,
        url = xmlUrl,
        synchronizedAt = null,
    )
}
