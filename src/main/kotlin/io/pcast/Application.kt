package io.pcast

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.pcast.controller.feed.createFeedHandler
import io.pcast.model.feed.FakeFeedRepository
import io.pcast.plugins.configureMonitoring

private val FEEDS = FakeFeedRepository()

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    createFeedHandler(FEEDS)
    configureMonitoring()
}
