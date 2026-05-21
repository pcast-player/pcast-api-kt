package io.pcast.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.pcast.error.AbortError
import io.pcast.error.ErrorResponse
import io.pcast.module.feed.error.FeedNotFoundError

fun Application.configureError() {
    install(StatusPages) {
        exception<AbortError> { call, cause ->
            val response = ErrorResponse(message = cause.details)

            call.respond(cause.code.statusCode, response)
        }

        exception<FeedNotFoundError> { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(message = "Feed not found"))
        }
    }
}
