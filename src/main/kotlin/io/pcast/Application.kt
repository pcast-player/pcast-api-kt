package io.pcast

import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.pcast.config.Configuration
import io.pcast.module.AppModule
import io.pcast.module.configModule
import io.pcast.module.dbModule
import io.pcast.plugins.configureAuth
import io.pcast.plugins.configureCors
import io.pcast.plugins.configureError
import io.pcast.plugins.configureMonitoring
import io.pcast.plugins.configureRateLimit
import io.pcast.plugins.configureRouting
import io.pcast.plugins.configureSecurityHeaders
import io.pcast.plugins.configureValidation
import org.koin.ksp.generated.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/**
 * Harden the StAX XMLInputFactory used by xmlutil against XXE and XML-bomb attacks.
 *
 * Must be called before any XML deserialization occurs. Disables:
 * - External general entities (XXE / SSRF vector)
 * - External parameter entities (DTD-based information disclosure)
 *
 * Works by setting JVM system properties honoured by both the JDK built-in
 * StAX RI and Woodstox (which is the factory used by xmlutil on JDK targets).
 *
 * IMPORTANT: Re-verify these settings on every xmlutil upgrade.
 * Current pin: xmlUtilVersion=0.91.3 (gradle.properties).
 */
internal fun hardenXmlParser() {
    System.setProperty("javax.xml.accessExternalDTD", "")
    System.setProperty("javax.xml.accessExternalSchema", "")
    System.setProperty("javax.xml.stream.supportDTD", "false")
    System.setProperty("jdk.xml.dtd.support", "deny")
}

fun Application.module() {
    hardenXmlParser()

    install(Koin) {
        slf4jLogger()
        modules(configModule, dbModule, AppModule().module)
    }

    val config by inject<Configuration>()
    configureCors(config.cors)
    configureSecurityHeaders()

    install(ContentNegotiation) {
        json()
        // xmlutil pin: 0.91.3 (gradle.properties). Review XXE hardening on upgrade.
        // ExperimentalXmlUtilApi: used via ktor-serialization-kotlinx-xml only;
        // no direct xmlutil API calls remain in application code.
        xml()
    }

    configureRateLimit()
    configureValidation()
    configureAuth()
    configureRouting()
    configureMonitoring()
    configureError()
}
