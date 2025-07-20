package io.pcast.service.feed

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.service.feed.opml.OpmlFile
import org.koin.ktor.ext.inject

fun Route.registerFeedRoutes() {
    val service by inject<FeedService>()

    get("/feeds") {
        val feeds = service.getFeeds()

        if (!feeds.isEmpty()) {
            call.respond(feeds.map(::FeedViewModel))
        } else {
            throw AbortError(HttpError.NoContent, "No feeds found.")
        }
    }

    post("/feeds") {
        try {
            val request = call.receive<FeedRequest>()
            val feed = service.addFeed(request)

            call.respond(HttpStatusCode.Created, FeedViewModel(feed))
        } catch (_: Throwable) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/feeds/{id}") {
        val id = call.parameters["id"] ?: throw AbortError(HttpError.BadRequest, "Feed ID must be provided")

        try {
            val feed = service.getFeed(id)

            call.respond(FeedViewModel(feed))
        } catch (_: Throwable) {
            throw AbortError(HttpError.NotFound, "No food found for ID $id")
        }
    }

    put("/feeds/{id}") {
        val id = call.parameters["id"] ?: throw AbortError(HttpError.BadRequest, "Feed ID must be provided")
        val request = call.receive<FeedRequest>()

        try {
            service.updateFeed(id, request)

            call.respond(HttpStatusCode.NoContent)
        } catch (_: Throwable) {
            throw AbortError(HttpError.NotFound, "No food found for ID $id")
        }
    }

    post("/feeds/opml") {
        val request = call.receive<OpmlFile>()

        try {
            val feeds = service.addFeeds(request).map(::FeedViewModel)

            call.respond(HttpStatusCode.Created, feeds)
        } catch (_: Throwable) {
            throw AbortError(HttpError.InternalError, "OPML import failed")
        }
    }
}
