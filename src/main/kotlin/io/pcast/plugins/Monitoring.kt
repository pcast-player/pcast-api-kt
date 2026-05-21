package io.pcast.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level

private val SENSITIVE_PATH_PREFIXES = listOf("/api/auth/", "/api/sync/")

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            val path = call.request.path()
            SENSITIVE_PATH_PREFIXES.none { path.startsWith(it) }
        }
    }
}
