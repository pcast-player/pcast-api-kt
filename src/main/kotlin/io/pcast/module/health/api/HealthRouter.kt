package io.pcast.module.health.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "ok",
)

fun Route.registerHealthRoutes() {
    get("/healthz") {
        call.respond(HttpStatusCode.OK, HealthResponse())
    }
}
