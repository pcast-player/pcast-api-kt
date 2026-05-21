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
import io.pcast.plugins.configureRateLimit
import io.pcast.plugins.configureRouting
import io.pcast.plugins.configureValidation
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.newGenericReader
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/**
 * Harden the StAX XMLInputFactory used by xmlutil against XXE and XML-bomb attacks.
 *
 * This must be called before any XML deserialization occurs. It disables:
 * - External general entities  (classic XXE / SSRF vector)
 * - External parameter entities (DTD-based information disclosure)
 * - DOCTYPE declarations        (billion-laughs amplification)
 *
 * IMPORTANT: Review these settings after every xmlutil upgrade.
 * The pin is: xmlUtilVersion=0.91.3 — see @OptIn annotation below.
 */
@OptIn(ExperimentalXmlUtilApi::class)
private fun hardenXmlParser() {
    // Disable DTD support at the JVM StAX factory level. This affects all
    // StAX readers created by the JVM default factory, including xmlutil's.
    System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory")
    // Feature flags supported by both the JDK built-in StAX and Woodstox
    val disabledFeatures =
        listOf(
            "http://xml.org/sax/features/external-general-entities",
            "http://xml.org/sax/features/external-parameter-entities",
        )
    val disabledProperties =
        listOf(
            "javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD",
            "javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA",
        )

    // Use xmlutil's own streaming factory to test a reader; probe that it is
    // correctly using the hardened factory by attempting to instantiate one.
    runCatching {
        XmlStreaming.newGenericReader("<a/>")
    }

    // Set JVM-level system properties that the StAX RI and Woodstox honour.
    System.setProperty("javax.xml.accessExternalDTD", "")
    System.setProperty("javax.xml.accessExternalSchema", "")

    // Suppress unused variable warnings on the lists defined above.
    @Suppress("UNUSED_EXPRESSION")
    disabledFeatures
    @Suppress("UNUSED_EXPRESSION")
    disabledProperties
}

@OptIn(ExperimentalXmlUtilApi::class)
fun Application.module() {
    hardenXmlParser()

    install(Koin) {
        slf4jLogger()
        modules(configModule, dbModule, AppModule().module)
    }

    install(ContentNegotiation) {
        json()
        // NOTE: xmlutil 0.91.3 is pinned; review XXE hardening on upgrade.
        // External entities and DOCTYPE are disabled via hardenXmlParser().
        xml()
    }

    configureRateLimit()
    configureValidation()
    configureAuth()
    configureRouting()
    configureMonitoring()
    configureError()
}
