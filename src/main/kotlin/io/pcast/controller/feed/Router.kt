package io.pcast.controller.feed

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.pcast.model.feed.FeedRepository
import io.pcast.result.Result
import io.pcast.result.isOk
import io.pcast.result.or
import io.pcast.result.toResult
import java.util.UUID

fun PipelineContext<Unit, ApplicationCall>.getId(): Result<UUID, Exception> =
    toResult { UUID.fromString(call.parameters["id"]) }

fun Application.createFeedHandler(repository: FeedRepository) {
    val handler = FeedHandler(repository)

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/api/feeds") {
            call.respond(handler.getFeeds())
        }

        post("/api/feeds") {
            val request = call.receive<FeedRequest>()
            val response = handler.addFeed(request)

            call.respond(HttpStatusCode.Created, response)
        }

        get("/api/feeds/{id}") {
            val id = getId() or { return@get call.respond(HttpStatusCode.BadRequest) }
            val feedResult = handler.getFeed(id)

            if (feedResult.isOk()) {
                call.respond(feedResult.value)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/api/feeds/{id}") {
            val id = getId() or { return@put call.respond(HttpStatusCode.BadRequest) }
            val request = call.receive<FeedRequest>()

            handler.updateFeed(id, request)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}