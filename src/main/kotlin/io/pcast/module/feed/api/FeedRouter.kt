package io.pcast.module.feed.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
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
import org.koin.ktor.ext.inject

fun Route.registerFeedRoutes() {
    val service by inject<FeedService>()

    get("/feeds") {
        val userId = call.userId()
        val feeds = service.getFeeds(userId)

        if (feeds.isNotEmpty()) {
            call.respond(feeds.map(::FeedResponse))
        } else {
            throw AbortError(HttpError.NoContent, "No feeds found.")
        }
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

    post("/feeds/opml") {
        val userId = call.userId()
        val request = call.receive<OpmlFile>()
        val feeds = service.addFeeds(request, userId).map(::FeedResponse)

        call.respond(HttpStatusCode.Created, feeds)
    }
}
