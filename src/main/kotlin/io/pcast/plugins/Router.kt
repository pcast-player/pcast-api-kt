package io.pcast.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.pcast.controller.feed.registerFeedRoutes

fun Application.configureRouting() {
    routing {
        route("/api") {
            registerFeedRoutes()
        }
    }
}
