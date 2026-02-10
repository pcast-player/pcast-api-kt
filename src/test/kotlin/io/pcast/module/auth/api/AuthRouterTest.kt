package io.pcast.module.auth.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.pcast.module.AppModule
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.request.RefreshRequest
import io.pcast.module.auth.response.TokenResponse
import io.pcast.module.configModule
import io.pcast.module.testDbModule
import io.pcast.plugins.configureAuth
import io.pcast.plugins.configureError
import io.pcast.plugins.configureRouting
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private const val TEST_EMAIL = "test@example.com"
private const val TEST_PASSWORD = "testpassword123"

internal class AuthRouterTest : KoinTest {
    private val authService by inject<AuthService>()

    @Test
    fun testLoginSuccess() =
        testApplication {
            val client = configureServerAndGetClient()

            client
                .post("/api/auth/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
                }.expect {
                    assertEquals(HttpStatusCode.OK, status)

                    val response = body<TokenResponse>()

                    assertNotNull(response.accessToken)
                    assertNotNull(response.refreshToken)
                    assertEquals("Bearer", response.tokenType)
                    assertTrue(response.expiresIn > 0)
                }
        }

    @Test
    fun testLoginInvalidEmail() =
        testApplication {
            val client = configureServerAndGetClient()

            client
                .post("/api/auth/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LoginRequest("wrong@example.com", TEST_PASSWORD))
                }.expect {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                }
        }

    @Test
    fun testLoginInvalidPassword() =
        testApplication {
            val client = configureServerAndGetClient()

            client
                .post("/api/auth/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LoginRequest(TEST_EMAIL, "wrongpassword"))
                }.expect {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                }
        }

    @Test
    fun testRefreshSuccess() =
        testApplication {
            val client = configureServerAndGetClient()

            // First login to get tokens
            val loginResponse =
                client
                    .post("/api/auth/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
                    }.body<TokenResponse>()

            // Refresh using the refresh token
            client
                .post("/api/auth/refresh") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(RefreshRequest(loginResponse.refreshToken))
                }.expect {
                    assertEquals(HttpStatusCode.OK, status)

                    val response = body<TokenResponse>()

                    assertNotNull(response.accessToken)
                    assertNotNull(response.refreshToken)
                    assertEquals("Bearer", response.tokenType)
                }
        }

    @Test
    fun testRefreshInvalidToken() =
        testApplication {
            val client = configureServerAndGetClient()

            client
                .post("/api/auth/refresh") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(RefreshRequest("invalid-refresh-token"))
                }.expect {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                }
        }

    @Test
    fun testRefreshTokenRotation() =
        testApplication {
            val client = configureServerAndGetClient()

            // Login to get tokens
            val loginResponse =
                client
                    .post("/api/auth/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
                    }.body<TokenResponse>()

            val originalRefreshToken = loginResponse.refreshToken

            // Refresh to get new tokens
            val refreshResponse =
                client
                    .post("/api/auth/refresh") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(RefreshRequest(originalRefreshToken))
                    }.expect {
                        assertEquals(HttpStatusCode.OK, status)
                    }.body<TokenResponse>()

            // Verify we got a new refresh token
            assertNotEquals(originalRefreshToken, refreshResponse.refreshToken)

            // Original refresh token should no longer work (rotation)
            client
                .post("/api/auth/refresh") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(RefreshRequest(originalRefreshToken))
                }.expect {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                }
        }

    @Test
    fun testLogoutSuccess() =
        testApplication {
            val client = configureServerAndGetClient()

            // Login to get tokens
            val loginResponse =
                client
                    .post("/api/auth/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
                    }.body<TokenResponse>()

            // Logout
            client
                .post("/api/auth/logout") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(RefreshRequest(loginResponse.refreshToken))
                }.expect {
                    assertEquals(HttpStatusCode.NoContent, status)
                }

            // Refresh token should no longer work
            client
                .post("/api/auth/refresh") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(RefreshRequest(loginResponse.refreshToken))
                }.expect {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                }
        }

    private inline fun HttpResponse.expect(test: HttpResponse.() -> Unit) = apply(test)

    private fun ApplicationTestBuilder.configureServerAndGetClient(): HttpClient {
        application {
            install(Koin) {
                modules(configModule, testDbModule, AppModule().module)
            }

            install(ContentNegotiation) {
                json()
            }

            seedTestUser()
            configureAuth()
            configureRouting()
            configureError()
        }

        return createClient {
            install(ClientContentNegotiation) {
                json()
            }
        }
    }

    private fun seedTestUser() {
        authService.createUser(TEST_EMAIL, TEST_PASSWORD)
    }
}
