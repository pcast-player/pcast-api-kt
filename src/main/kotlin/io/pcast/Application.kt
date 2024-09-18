package io.pcast

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.pcast.model.FakeFeedRepository
import io.pcast.plugins.configureFeed
import io.pcast.plugins.configureMonitoring

private val FEEDS = FakeFeedRepository()

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureFeed(FEEDS)
    configureMonitoring()
}
