package io.pcast.service.feed

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.pcast.result.attempt
import io.pcast.result.or
import io.pcast.result.unwrap
import io.pcast.service.feed.opml.OpmlFile
import org.koin.ktor.ext.inject

fun Route.registerFeedRoutes() {
    val service by inject<FeedService>()

    get("/feeds") {
        attempt {
            val result = service.getFeeds()
            val feeds = result.unwrap(::FeedViewModel)

            call.respond(feeds)
        } or {
            call.respond(HttpStatusCode.NoContent)
        }
    }

    post("/feeds") {
        attempt {
            val request = call.receive<FeedRequest>()
            val result = service.addFeed(request)
            val feed = result.unwrap(::FeedViewModel)

            call.respond(HttpStatusCode.Created, feed)
        } or {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/feeds/{id}") {
        attempt {
            val id = call.parameters["id"]

            requireNotNull(id) { "Feed ID must be provided" }

            val result = service.getFeed(id)
            val feed = result.unwrap(::FeedViewModel)

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

            service.updateFeed(id, request)

            call.respond(HttpStatusCode.NoContent)
        } or {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    post("/feeds/opml") {
        attempt {
            val request = call.receive<OpmlFile>()
            val feeds = service.addFeeds(request).unwrap(::FeedViewModel)

            call.respond(HttpStatusCode.Created, feeds)
        } or {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
