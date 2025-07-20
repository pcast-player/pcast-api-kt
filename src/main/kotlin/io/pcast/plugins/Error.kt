package io.pcast.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.pcast.error.AbortError
import io.pcast.error.ErrorViewModel

fun Application.configureError() {
    install(StatusPages) {
        exception<AbortError> { call, cause ->
            val response = ErrorViewModel(message = cause.details)

            call.respond(cause.code.statusCode, response)
        }
    }
}
