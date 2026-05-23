package io.pcast.module.feed.api

import com.fasterxml.uuid.Generators
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
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
import io.pcast.hardenXmlParser
import io.pcast.helpers.generateNanoId
import io.pcast.helpers.generateUuidV7
import io.pcast.module.AppModule
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.response.TokenResponse
import io.pcast.module.feed.model.Feed
import io.pcast.module.feed.model.FeedRepository
import io.pcast.module.feed.request.FeedRequest
import io.pcast.module.feed.response.FeedResponse
import io.pcast.module.testConfigModule
import io.pcast.module.testDbModule
import io.pcast.plugins.configureAuth
import io.pcast.plugins.configureError
import io.pcast.plugins.configureRateLimit
import io.pcast.plugins.configureRouting
import io.pcast.plugins.configureValidation
import org.koin.ksp.generated.module
import org.koin.ktor.plugin.Koin
import org.koin.test.KoinTest
import org.koin.test.inject
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private val BASE_DATE = LocalDateTime.now()

private fun feed(
    i: Int,
    userId: UUID,
) = Feed(
    id = generateUuidV7(),
    userId = userId,
    nanoId = generateNanoId(),
    title = "Feed $i",
    url = "https://rss.pcast.io/news$i.rss",
    synchronizedAt = BASE_DATE.minusDays(i).truncatedTo(ChronoUnit.SECONDS),
)

private const val TEST_EMAIL = "feedtest@example.com"
private const val TEST_PASSWORD = "testpassword123"
private const val TEST_EMAIL_2 = "feedtest2@example.com"
private const val TEST_PASSWORD_2 = "testpassword456"

private data class TestContext(
    val client: HttpClient,
    val accessToken: String,
    val userId: UUID,
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
                    val feeds = body<List<FeedResponse>>()
                    assertEquals(10, feeds.size)
                }
        }

    @Test
    fun testGetFeedsReturnsEmptyList() =
        testApplication {
            val ctx = configureServerAndGetContext(seedFeeds = false)

            ctx.client
                .get("/api/feeds") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(emptyList(), body<List<FeedResponse>>())
                }
        }

    @Test
    fun testGetFeed() =
        testApplication {
            val ctx = configureServerAndGetContext()
            val feeds = feedRepository.findAll(ctx.userId)
            val feed = feeds.first()
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
            val ctx = configureServerAndGetContext()
            val feeds = feedRepository.findAll(ctx.userId)
            val feed = feeds.first()
            val newTitle = "new title"

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

    @Test
    fun testDeleteFeed() =
        testApplication {
            val ctx = configureServerAndGetContext()
            val feed = feedRepository.findAll(ctx.userId).first()

            ctx.client
                .delete("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.NoContent, status)
                }

            ctx.client
                .get("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }

    @Test
    fun testUserCannotDeleteOtherUsersFeed() =
        testApplication {
            val ctx1 = configureServerAndGetContext()
            val ctx2 = loginSecondUser(ctx1.client)
            val feed = feedRepository.findAll(ctx1.userId).first()

            ctx1.client
                .delete("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx2)
                }.expect {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }

    @Test
    fun testUserCannotAccessOtherUsersFeed() =
        testApplication {
            val ctx1 = configureServerAndGetContext()
            val ctx2 = loginSecondUser(ctx1.client)

            // ctx2 tries to read ctx1's feed
            val feeds = feedRepository.findAll(ctx1.userId)
            val feed = feeds.first()

            ctx1.client
                .get("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx2)
                }.expect {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }

    @Test
    fun testUserCannotUpdateOtherUsersFeed() =
        testApplication {
            val ctx1 = configureServerAndGetContext()
            val ctx2 = loginSecondUser(ctx1.client)

            val feeds = feedRepository.findAll(ctx1.userId)
            val feed = feeds.first()

            ctx1.client
                .put("/api/feeds/${feed.nanoId}") {
                    bearerAuth(ctx2)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(FeedRequest("hacked", feed.url))
                }.expect {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }

    @Test
    fun testDifferentUsersCanHaveSameNanoId() =
        testApplication {
            val ctx = configureServerAndGetContext()
            val secondUser = authService.getUserByEmail(TEST_EMAIL_2)!!
            val sharedNanoId = "shared-nano-id-001"

            feedRepository.create(feed(101, ctx.userId).copy(nanoId = sharedNanoId, title = "User 1 shared feed"))
            feedRepository.create(feed(102, secondUser.id).copy(nanoId = sharedNanoId, title = "User 2 shared feed"))

            assertEquals("User 1 shared feed", feedRepository.findByNanoId(sharedNanoId, ctx.userId).title)
            assertEquals("User 2 shared feed", feedRepository.findByNanoId(sharedNanoId, secondUser.id).title)
        }

    @Test
    fun testOpmlImportRejectsDoctype() =
        testApplication {
            val ctx = configureServerAndGetContext()

            ctx.client
                .post("/api/feeds/opml") {
                    bearerAuth(ctx.accessToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Xml.toString())
                    setBody(
                        """
                        <!DOCTYPE opml [<!ENTITY xxe "expanded">]>
                        <opml version="2.0">
                          <head><title>&xxe;</title></head>
                          <body>
                            <outline text="feeds">
                              <outline text="&xxe;" type="rss" xmlUrl="https://rss.pcast.io/news.rss" />
                            </outline>
                          </body>
                        </opml>
                        """.trimIndent(),
                    )
                }.expect {
                    assertEquals(HttpStatusCode.BadRequest, status)
                }
        }

    private inline fun HttpResponse.expect(test: HttpResponse.() -> Unit) = apply(test)

    private suspend fun ApplicationTestBuilder.configureServerAndGetContext(seedFeeds: Boolean = true): TestContext {
        application {
            hardenXmlParser()

            install(Koin) {
                modules(testConfigModule, testDbModule, AppModule().module)
            }

            install(ContentNegotiation) {
                json()
                xml()
            }

            seedTestUsers()
            configureRateLimit()
            configureValidation()
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

        val tokenResponse =
            client
                .post("/api/auth/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
                }.body<TokenResponse>()

        val user = authService.getUserByEmail(TEST_EMAIL)!!
        if (seedFeeds) {
            addTestData(user.id)
        }

        return TestContext(client, tokenResponse.accessToken, user.id)
    }

    private suspend fun loginSecondUser(client: HttpClient): String =
        client
            .post("/api/auth/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(LoginRequest(TEST_EMAIL_2, TEST_PASSWORD_2))
            }.body<TokenResponse>()
            .accessToken

    private fun addTestData(userId: UUID) {
        for (i in 1..10) {
            feedRepository.create(feed(i, userId))
        }
    }

    private fun seedTestUsers() {
        authService.createUser(TEST_EMAIL, TEST_PASSWORD)
        authService.createUser(TEST_EMAIL_2, TEST_PASSWORD_2)
    }
}
