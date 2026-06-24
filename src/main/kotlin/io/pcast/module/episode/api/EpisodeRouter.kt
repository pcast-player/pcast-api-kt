package io.pcast.module.episode.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.extensions.userId
import io.pcast.module.episode.EpisodeService
import io.pcast.module.episode.request.EpisodeProgressRequest
import io.pcast.module.episode.response.EpisodeResponse
import org.koin.ktor.ext.inject
import java.time.LocalDateTime
import java.util.UUID

fun Route.registerEpisodeRoutes() {
    val service by inject<EpisodeService>()

    get("/episodes") {
        val feedId = call.request.queryParameters["feedId"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()
        val before =
            call.request.queryParameters["before"]?.let {
                runCatching { LocalDateTime.parse(it) }
                    .getOrElse { throw AbortError(HttpError.BadRequest, "before must be an ISO local date time") }
            }

        call.respond(service.listEpisodes(call.userId(), feedId, limit, before).map(::EpisodeResponse))
    }

    get("/episodes/{id}") {
        val episodeId = call.episodeId()

        call.respond(EpisodeResponse(service.getEpisode(episodeId, call.userId())))
    }

    put("/episodes/{id}/progress") {
        val episodeId = call.episodeId()
        val request = call.receive<EpisodeProgressRequest>()

        service.updateProgress(episodeId, call.userId(), request)

        call.respond(HttpStatusCode.NoContent)
    }
}

private fun io.ktor.server.application.ApplicationCall.episodeId(): UUID =
    parameters["id"]
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw AbortError(HttpError.BadRequest, "Episode ID must be provided")
