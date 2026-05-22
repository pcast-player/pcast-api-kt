package io.pcast.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.pcast.module.auth.api.registerAuthRoutes
import io.pcast.module.feed.api.registerFeedRoutes
import io.pcast.module.health.api.registerHealthRoutes
import io.pcast.module.sync.api.registerSyncRoutes

fun Application.configureRouting() {
    routing {
        registerHealthRoutes()

        route("/api") {
            registerAuthRoutes()

            authenticate(JWT_AUTH_NAME) {
                registerFeedRoutes()
                registerSyncRoutes()
            }
        }
    }
}
