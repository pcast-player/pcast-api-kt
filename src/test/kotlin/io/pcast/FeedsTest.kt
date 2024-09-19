package io.pcast

import com.fasterxml.uuid.Generators
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.pcast.model.FakeFeedRepository
import io.pcast.plugins.configureFeed
import io.pcast.request.FeedRequest
import io.pcast.response.FeedResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val FEEDS = FakeFeedRepository()

internal class FeedsTest {
    @Test
    fun testGetFeeds() = testApplication {
        val client = configureServerAndGetClient()

        client.get("/api/feeds").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(FEEDS.findAll().map(::FeedResponse), body<List<FeedResponse>>())
        }
    }

    @Test
    fun testGetFeed() = testApplication {
        val client = configureServerAndGetClient()
        val feed = FEEDS.findAll().first()
        val response = FeedResponse(feed)

        client.get("/api/feeds/${feed.id}").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(response, body<FeedResponse>())
        }
    }

    @Test
    fun testGetFeedFailsWithUnknownId() = testApplication {
        val client = configureServerAndGetClient()
        val uuid = Generators.timeBasedGenerator().generate()

        client.get("/api/feeds/$uuid").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun testGetFeedFailsWithWrongIdType() = testApplication {
        val client = configureServerAndGetClient()

        client.get("/api/feeds/fdsfsdf").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testCreateFeed() = testApplication {
        val title = "title"
        val url = "https://foo.bar"
        val client = configureServerAndGetClient()

        client.post("/api/feeds") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(FeedRequest(title, url))
        }.apply {
            assertEquals(HttpStatusCode.Created, status)

            val response = body<FeedResponse>()

            assertEquals(title, response.title)
            assertEquals(url, response.url)
            assertNull(response.synchronizedAt)
        }
    }

    private fun ApplicationTestBuilder.configureServerAndGetClient(): HttpClient {
        application {
            configureFeed(FEEDS)
        }

        return createClient {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}
