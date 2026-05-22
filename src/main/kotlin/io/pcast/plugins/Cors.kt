package io.pcast.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.pcast.config.CorsConfig
import io.pcast.module.PCAST_ENV
import org.slf4j.LoggerFactory
import java.net.URI

private val log = LoggerFactory.getLogger("io.pcast.plugins.Cors")

private data class AllowedOrigin(
    val scheme: String,
    val host: String,
)

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
        config.allowedOrigins.forEach { origin ->
            val parsed = parseAllowedOrigin(origin)
            allowHost(parsed.host, schemes = listOf(parsed.scheme))
        }

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

private fun parseAllowedOrigin(origin: String): AllowedOrigin {
    val uri =
        runCatching { URI(origin) }
            .getOrElse { throw IllegalArgumentException("Invalid CORS allowed origin: $origin", it) }
    val scheme = uri.scheme?.lowercase()
    require(scheme == "http" || scheme == "https") {
        "CORS allowed origin must use http or https: $origin"
    }
    require(!uri.host.isNullOrBlank()) {
        "CORS allowed origin must include a host: $origin"
    }
    require(uri.rawPath.isNullOrEmpty() && uri.rawQuery == null && uri.rawFragment == null && uri.rawUserInfo == null) {
        "CORS allowed origin must not include path, query, fragment, or user info: $origin"
    }

    val host = if (":" in uri.host && !uri.host.startsWith("[")) "[${uri.host}]" else uri.host
    val hostWithPort = if (uri.port >= 0) "$host:${uri.port}" else host
    return AllowedOrigin(scheme = scheme, host = hostWithPort)
}
