package io.pcast.module.feed.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.extensions.userId
import io.pcast.module.feed.FeedService
import io.pcast.module.feed.opml.OpmlFile
import io.pcast.module.feed.request.FeedRequest
import io.pcast.module.feed.response.FeedResponse
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
import org.koin.ktor.ext.inject

private const val DEFAULT_PAGE = 1
private const val DEFAULT_PAGE_SIZE = 50
private const val MAX_PAGE_SIZE = 100

private val OPML_XML =
    XML {
        repairNamespaces = true
        xmlDeclMode = nl.adaptivity.xmlutil.XmlDeclMode.None
        indentString = ""
        autoPolymorphic = false
    }

fun Route.registerFeedRoutes() {
    val service by inject<FeedService>()

    get("/feeds") {
        val userId = call.userId()
        val (page, pageSize) = call.paginationParams()
        val result = service.getFeeds(userId, page, pageSize)

        call.response.header("X-Total-Count", result.total.toString())
        call.respond(result.feeds.map(::FeedResponse))
    }

    post("/feeds") {
        val userId = call.userId()
        val request = call.receive<FeedRequest>()
        val feed = service.addFeed(request, userId)

        call.respond(HttpStatusCode.Created, FeedResponse(feed))
    }

    get("/feeds/{id}") {
        val id = call.parameters["id"] ?: throw AbortError(HttpError.BadRequest, "Feed ID must be provided")
        val userId = call.userId()
        val feed = service.getFeed(id, userId)

        call.respond(FeedResponse(feed))
    }

    put("/feeds/{id}") {
        val id = call.parameters["id"] ?: throw AbortError(HttpError.BadRequest, "Feed ID must be provided")
        val userId = call.userId()
        val request = call.receive<FeedRequest>()

        service.updateFeed(id, request, userId)

        call.respond(HttpStatusCode.NoContent)
    }

    delete("/feeds/{id}") {
        val id = call.parameters["id"] ?: throw AbortError(HttpError.BadRequest, "Feed ID must be provided")
        val userId = call.userId()

        service.deleteFeed(id, userId)

        call.respond(HttpStatusCode.NoContent)
    }

    post("/feeds/opml") {
        val userId = call.userId()
        val request = call.receiveOpmlFile()
        val feeds = service.addFeeds(request, userId).map(::FeedResponse)

        call.respond(HttpStatusCode.Created, feeds)
    }
}

/**
 * Parses 1-based `page` and `pageSize` query parameters, falling back to defaults when absent.
 * Rejects non-numeric values, page < 1, pageSize < 1, and pageSize beyond [MAX_PAGE_SIZE] with 400.
 */
private fun io.ktor.server.application.ApplicationCall.paginationParams(): Pair<Int, Int> {
    val page = parsePositiveIntParam("page", DEFAULT_PAGE)
    val pageSize = parsePositiveIntParam("pageSize", DEFAULT_PAGE_SIZE)

    if (pageSize > MAX_PAGE_SIZE) {
        throw AbortError(HttpError.BadRequest, "pageSize must not exceed $MAX_PAGE_SIZE")
    }

    return page to pageSize
}

private fun io.ktor.server.application.ApplicationCall.parsePositiveIntParam(
    name: String,
    default: Int,
): Int {
    val raw = request.queryParameters[name] ?: return default
    val value = raw.toIntOrNull() ?: throw AbortError(HttpError.BadRequest, "$name must be an integer")
    if (value < 1) throw AbortError(HttpError.BadRequest, "$name must be >= 1")
    return value
}

private suspend fun io.ktor.server.application.ApplicationCall.receiveOpmlFile(): OpmlFile {
    val body = receiveText()
    if (body.contains("<!DOCTYPE", ignoreCase = true)) {
        throw AbortError(HttpError.BadRequest, "OPML must not include a DOCTYPE declaration")
    }

    return runCatching { OPML_XML.decodeFromString<OpmlFile>(body) }
        .getOrElse { throw AbortError(HttpError.BadRequest, "Invalid OPML request body", it) }
}
