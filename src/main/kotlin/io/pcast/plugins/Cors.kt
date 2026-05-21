package io.pcast.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.pcast.config.CorsConfig
import io.pcast.module.PCAST_ENV
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("io.pcast.plugins.Cors")

/**
 * Install CORS with a default-deny policy: only origins listed in
 * [CorsConfig.allowedOrigins] are permitted.
 *
 * In production, an empty allow-list means every cross-origin preflight is
 * rejected (403). The warning below reminds operators to configure it.
 */
fun Application.configureCors(config: CorsConfig) {
    if (config.allowedOrigins.isEmpty()) {
        if (PCAST_ENV == "production") {
            log.warn(
                "CORS: cors.allowedOrigins is empty in production — all cross-origin requests will be blocked. " +
                    "Set cors.allowedOrigins in app.prod.conf if a web client needs access.",
            )
        }
        // Nothing to install — CORS plugin absent = no cross-origin requests allowed.
        return
    }

    install(CORS) {
        config.allowedOrigins.forEach { origin -> allowHost(origin, schemes = listOf("https", "http")) }

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowCredentials = true
    }
}
