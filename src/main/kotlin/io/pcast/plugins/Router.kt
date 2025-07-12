package io.pcast.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.pcast.service.feed.registerFeedRoutes
import io.pcast.service.sync.registerSyncRoutes

fun Application.configureRouting() {
    routing {
        route("/api") {
            registerFeedRoutes()
            registerSyncRoutes()
        }
    }
}
