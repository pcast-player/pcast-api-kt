package io.pcast

import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.pcast.config.loadConfiguration
import io.pcast.model.feed.FeedRepositoryImpl
import io.pcast.plugins.configureDatabase
import io.pcast.plugins.configureMonitoring
import io.pcast.plugins.configureRouting
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@OptIn(ExperimentalXmlUtilApi::class)
fun Application.module() {
    install(ContentNegotiation) {
        json()
        xml()
    }

    val config = loadConfiguration()
    val db = configureDatabase(config)

    configureRouting(FeedRepositoryImpl(db))
    configureMonitoring()
}
