package io.pcast.module.feed.api

import com.fasterxml.uuid.Generators
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.pcast.extensions.minusDays
import io.pcast.helpers.generateNanoId
import io.pcast.helpers.generateUuidV7
import io.pcast.module.AppModule
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.response.TokenResponse
import io.pcast.module.configModule
import io.pcast.module.feed.model.Feed
import io.pcast.module.feed.model.FeedRepository
import io.pcast.module.feed.request.FeedRequest
import io.pcast.module.feed.response.FeedResponse
import io.pcast.module.testDbModule
import io.pcast.plugins.configureAuth
import io.pcast.plugins.configureError
import io.pcast.plugins.configureRouting
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private val BASE_DATE = LocalDateTime.now()

private fun feed(i: Int) =
    Feed(
        id = generateUuidV7(),
        nanoId = generateNanoId(),
        title = "Feed $i",
        url = "https://rss.pcast.io/news$i.rss",
        synchronizedAt = BASE_DATE.minusDays(i).truncatedTo(ChronoUnit.SECONDS),
    )

private val FEEDS =
    buildList {
        for (i in 1..10) {
            add(feed(i))
        }
    }

private const val TEST_EMAIL = "feedtest@example.com"
private const val TEST_PASSWORD = "testpassword123"

private data class TestContext(
    val client: HttpClient,
    val accessToken: String,
)

internal class FeedRouterTest : KoinTest {
    private val feedRepository by inject<FeedRepository>()
    private val authService by inject<AuthService>()

    @Test
    fun testGetFeeds() =
        testApplication {
            val ctx = configureServerAndGetContext()

            ctx.client
                .get("/api/feeds") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(FEEDS.map(::FeedResponse), body<List<FeedResponse>>())
                }
        }

    @Test
    fun testGetFeed() =
        testApplication {
            val ctx = configureServerAndGetContext()
            val feed = FEEDS.first()
            val response = FeedResponse(feed)

            ctx.client
                .get("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(response, body<FeedResponse>())
                }
        }

    @Test
    fun testGetFeedFailsWithUnknownId() =
        testApplication {
            val ctx = configureServerAndGetContext()
            val uuid = Generators.timeBasedGenerator().generate()

            ctx.client
                .get("/api/feeds/$uuid") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }

    @Test
    fun testGetFeedFailsWithWrongIdType() =
        testApplication {
            val ctx = configureServerAndGetContext()

            ctx.client
                .get("/api/feeds/fdsfsdf") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }

    @Test
    fun testCreateFeed() =
        testApplication {
            val title = "title"
            val url = "https://foo.bar"
            val ctx = configureServerAndGetContext()

            ctx.client
                .post("/api/feeds") {
                    bearerAuth(ctx.accessToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(FeedRequest(title, url))
                }.expect {
                    assertEquals(HttpStatusCode.Created, status)

                    val response = body<FeedResponse>()

                    assertEquals(title, response.title)
                    assertEquals(url, response.url)
                    assertNull(response.synchronizedAt)
                }
        }

    @Test
    fun testUpdateFeed() =
        testApplication {
            val feed = FEEDS.first()
            val newTitle = "new title"
            val ctx = configureServerAndGetContext()

            ctx.client
                .put("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx.accessToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(FeedRequest(newTitle, feed.url))
                }.expect {
                    assertEquals(HttpStatusCode.NoContent, status)
                }

            ctx.client
                .get("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.OK, status)

                    val response = body<FeedResponse>()

                    assertEquals(newTitle, response.title)
                }
        }

    private inline fun HttpResponse.expect(test: HttpResponse.() -> Unit) = apply(test)

    private suspend fun ApplicationTestBuilder.configureServerAndGetContext(): TestContext {
        application {
            install(Koin) {
                modules(configModule, testDbModule, AppModule().module)
            }

            install(ContentNegotiation) {
                json()
            }

            addTestData()
            seedTestUser()
            configureAuth()
            configureRouting()
            configureError()
        }

        val client =
            createClient {
                install(ClientContentNegotiation) {
                    json()
                    xml()
                }
            }

        // Login via HTTP to get a valid token
        val tokenResponse =
            client
                .post("/api/auth/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
                }.body<TokenResponse>()

        return TestContext(client, tokenResponse.accessToken)
    }

    private fun addTestData() {
        for (feed in FEEDS) {
            feedRepository.save(feed)
        }
    }

    private fun seedTestUser() {
        authService.createUser(TEST_EMAIL, TEST_PASSWORD)
    }
}
