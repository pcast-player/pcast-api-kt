package io.pcast.module.episode.api

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
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.pcast.helpers.generateNanoId
import io.pcast.helpers.generateUuidV7
import io.pcast.module.AppModule
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.response.TokenResponse
import io.pcast.module.episode.model.Episode
import io.pcast.module.episode.model.EpisodeRepository
import io.pcast.module.episode.request.EpisodeProgressRequest
import io.pcast.module.episode.response.EpisodeResponse
import io.pcast.module.feed.model.Feed
import io.pcast.module.feed.model.FeedRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private const val TEST_EMAIL = "episodetest@example.com"
private const val TEST_PASSWORD = "testpassword123"
private const val TEST_EMAIL_2 = "episodetest2@example.com"
private const val TEST_PASSWORD_2 = "testpassword456"

private data class TestContext(
    val client: HttpClient,
    val accessToken: String,
    val userId: UUID,
    val feed: Feed,
    val episode: Episode,
)

internal class EpisodeRouterTest : KoinTest {
    private val authService by inject<AuthService>()
    private val feedRepository by inject<FeedRepository>()
    private val episodeRepository by inject<EpisodeRepository>()

    @Test
    fun testGetEpisodesReturnsOwnedEpisodesWithProgress() =
        testApplication {
            val ctx = configureServerAndGetContext()

            ctx.client
                .put("/api/episodes/${ctx.episode.id}/progress") {
                    bearerAuth(ctx.accessToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(EpisodeProgressRequest(positionSeconds = 30, durationSeconds = 120, completed = false))
                }.expect {
                    assertEquals(HttpStatusCode.NoContent, status)
                }

            ctx.client
                .get("/api/episodes?feedId=${ctx.feed.nanoId}") {
                    bearerAuth(ctx.accessToken)
                }.expect {
                    assertEquals(HttpStatusCode.OK, status)

                    val episodes = body<List<EpisodeResponse>>()
                    assertEquals(1, episodes.size)
                    assertEquals(ctx.episode.id, episodes.single().id)
                    assertEquals(ctx.feed.nanoId, episodes.single().feedNanoId)
                    assertNotNull(episodes.single().progress)
                    assertEquals(30, episodes.single().progress?.positionSeconds)
                }
        }

    @Test
    fun testGetEpisodeRejectsOtherUsersEpisode() =
        testApplication {
            val ctx = configureServerAndGetContext()
            val otherToken = loginSecondUser(ctx.client)

            ctx.client
                .get("/api/episodes/${ctx.episode.id}") {
                    bearerAuth(otherToken)
                }.expect {
                    assertEquals(HttpStatusCode.NotFound, status)
                }
        }

    @Test
    fun testProgressRejectsPositionAfterDurationDriftAllowance() =
        testApplication {
            val ctx = configureServerAndGetContext()

            ctx.client
                .put("/api/episodes/${ctx.episode.id}/progress") {
                    bearerAuth(ctx.accessToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(EpisodeProgressRequest(positionSeconds = 126, durationSeconds = 120, completed = false))
                }.expect {
                    assertEquals(HttpStatusCode.BadRequest, status)
                }
        }

    @Test
    fun testProgressAllowsSmallPlayerDrift() =
        testApplication {
            val ctx = configureServerAndGetContext()

            ctx.client
                .put("/api/episodes/${ctx.episode.id}/progress") {
                    bearerAuth(ctx.accessToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(EpisodeProgressRequest(positionSeconds = 125, durationSeconds = 120, completed = false))
                }.expect {
                    assertEquals(HttpStatusCode.NoContent, status)
                }
        }

    @Test
    fun testCompletedProgressRequiresDuration() =
        testApplication {
            val ctx = configureServerAndGetContext(episodeDuration = null)

            ctx.client
                .put("/api/episodes/${ctx.episode.id}/progress") {
                    bearerAuth(ctx.accessToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(EpisodeProgressRequest(positionSeconds = 30, durationSeconds = null, completed = true))
                }.expect {
                    assertEquals(HttpStatusCode.BadRequest, status)
                }
        }

    private inline fun HttpResponse.expect(test: HttpResponse.() -> Unit) = apply(test)

    private suspend fun ApplicationTestBuilder.configureServerAndGetContext(episodeDuration: Long? = 120): TestContext {
        application {
            install(Koin) {
                modules(testConfigModule, testDbModule, AppModule().module)
            }

            install(ContentNegotiation) {
                json()
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
                }
            }

        val tokenResponse =
            client
                .post("/api/auth/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
                }.body<TokenResponse>()

        val user = authService.getUserByEmail(TEST_EMAIL)!!
        val otherUser = authService.getUserByEmail(TEST_EMAIL_2)!!
        val feed = createFeed(user.id, "Owned")
        val otherFeed = createFeed(otherUser.id, "Other")
        val episode = createEpisode(feed.id, "owned-episode", episodeDuration)
        createEpisode(otherFeed.id, "other-episode", 120)

        return TestContext(client, tokenResponse.accessToken, user.id, feed, episode)
    }

    private suspend fun loginSecondUser(client: HttpClient): String =
        client
            .post("/api/auth/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(LoginRequest(TEST_EMAIL_2, TEST_PASSWORD_2))
            }.body<TokenResponse>()
            .accessToken

    private fun createFeed(
        userId: UUID,
        title: String,
    ): Feed {
        val feed =
            Feed(
                id = generateUuidV7(),
                userId = userId,
                nanoId = generateNanoId(),
                title = title,
                url = "https://example.com/$title.xml",
                synchronizedAt = null,
            )
        feedRepository.create(feed)
        return feed
    }

    private fun createEpisode(
        feedId: UUID,
        guid: String,
        duration: Long?,
    ): Episode {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        val episode =
            Episode(
                id = generateUuidV7(),
                feedId = feedId,
                guid = guid,
                title = "Episode $guid",
                description = "Description $guid",
                mediaUrl = "https://cdn.example.com/$guid.mp3",
                mediaType = "audio/mpeg",
                durationSeconds = duration,
                publishedAt = now,
                imageUrl = "https://example.com/$guid.jpg",
                createdAt = now,
                updatedAt = now,
            )
        episodeRepository.upsert(episode)
        return episode
    }

    private fun seedTestUsers() {
        authService.createUser(TEST_EMAIL, TEST_PASSWORD)
        authService.createUser(TEST_EMAIL_2, TEST_PASSWORD_2)
    }
}
