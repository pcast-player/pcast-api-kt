package io.pcast

import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.pcast.module.AppModule
import io.pcast.module.configModule
import io.pcast.module.dbModule
import io.pcast.plugins.configureAuth
import io.pcast.plugins.configureError
import io.pcast.plugins.configureMonitoring
import io.pcast.plugins.configureRouting
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.koin.ksp.generated.module
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
        modules(configModule, dbModule, AppModule().module)
    }

    install(ContentNegotiation) {
        json()
        xml()
    }

    configureAuth()
    configureRouting()
    configureMonitoring()
    configureError()
}
