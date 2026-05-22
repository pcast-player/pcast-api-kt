package io.pcast.plugins

import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.pcast.config.CorsConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class CorsTest {
    @Test
    fun testAllowedExactOrigin() =
        testApplication {
            application {
                configureCors(CorsConfig(allowedOrigins = listOf("https://app.example.com")))
                testRoute()
            }

            val response =
                client.options("/test") {
                    header(HttpHeaders.Origin, "https://app.example.com")
                    header(HttpHeaders.AccessControlRequestMethod, HttpMethod.Get.value)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("https://app.example.com", response.headers[HttpHeaders.AccessControlAllowOrigin])
        }

    @Test
    fun testRejectedMismatchedOrigin() =
        testApplication {
            application {
                configureCors(CorsConfig(allowedOrigins = listOf("https://app.example.com")))
                testRoute()
            }

            val response =
                client.options("/test") {
                    header(HttpHeaders.Origin, "https://evil.example.com")
                    header(HttpHeaders.AccessControlRequestMethod, HttpMethod.Get.value)
                }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun testMalformedAllowedOriginFailsStartup() {
        assertFailsWith<IllegalArgumentException> {
            testApplication {
                application {
                    configureCors(CorsConfig(allowedOrigins = listOf("https://app.example.com/path")))
                    testRoute()
                }
            }
        }
    }

    private fun Application.testRoute() {
        routing {
            get("/test") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
