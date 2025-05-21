package io.pcast.controller.feed

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.pcast.controller.feed.opml.OpmlFile
import io.pcast.model.feed.FeedRepository
import io.pcast.result.attempt
import io.pcast.result.or
import io.pcast.result.unwrap

fun Route.registerFeedRoutes(
    repository: FeedRepository
) {
    val handler = FeedHandler(repository)

    get("/feeds") {
        attempt {
            val result = handler.getFeeds()
            val feeds = result.unwrap(::FeedResponse)

            call.respond(feeds)
        } or {
            call.respond(HttpStatusCode.NoContent)
        }
    }

    post("/feeds") {
        attempt {
            val request = call.receive<FeedRequest>()
            val result = handler.addFeed(request)
            val feed = result.unwrap(::FeedResponse)

            call.respond(HttpStatusCode.Created, feed)
        } or {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/feeds/{id}") {
        attempt {
            val id = call.parameters["id"]

            requireNotNull(id) { "Feed ID must be provided" }

            val result = handler.getFeed(id)
            val feed = result.unwrap(::FeedResponse)

            call.respond(feed)
        } or {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    put("/feeds/{id}") {
        attempt {
            val id = call.parameters["id"]

            requireNotNull(id) { "Feed ID must be provided" }

            val request = call.receive<FeedRequest>()

            handler.updateFeed(id, request)

            call.respond(HttpStatusCode.NoContent)
        } or {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    post("/feeds/opml") {
        attempt {
            val request = call.receive<OpmlFile>()
            val feeds = handler.addFeeds(request).unwrap(::FeedResponse)

            call.respond(HttpStatusCode.Created, feeds)
        } or {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
