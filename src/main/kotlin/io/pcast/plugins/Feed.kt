package io.pcast.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.pcast.model.FeedRepository
import io.pcast.result.Result
import io.pcast.result.isOk

fun PipelineContext<Unit, ApplicationCall>.getIntParameter(name: String): Result<Int, Exception> {
    val rawParam = call.parameters["id"] ?: return Result.error(Exception("Missing parameter $name"))
    val param = rawParam.toIntOrNull() ?: return Result.error(Exception("Could not parse value '$rawParam' to Int"))

    return Result.ok(param)
}

fun Application.configureFeed(repository: FeedRepository) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/api/feeds") {
            call.respond(repository.findAll())
        }

        get("/api/feeds/{id}") {
            val idResult = getIntParameter("id")

            if (idResult.isOk()) {
                val (id) = idResult
                val feedResult = repository.find(id)

                if (feedResult.isOk()) {
                    call.respond(feedResult.value)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}