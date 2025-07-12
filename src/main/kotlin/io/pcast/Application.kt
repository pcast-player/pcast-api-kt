package io.pcast

import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.pcast.di.appModule
import io.pcast.di.configModule
import io.pcast.di.dbModule
import io.pcast.plugins.configureMonitoring
import io.pcast.plugins.configureRouting
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@OptIn(ExperimentalXmlUtilApi::class)
fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(configModule, dbModule, appModule)
    }

    install(ContentNegotiation) {
        json()
        xml()
    }

    configureRouting()
    configureMonitoring()
}
