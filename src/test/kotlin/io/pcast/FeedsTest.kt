package io.pcast

import com.fasterxml.uuid.Generators
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.pcast.controller.feed.FeedRequest
import io.pcast.controller.feed.FeedResponse
import io.pcast.model.feed.FakeFeedRepository
import io.pcast.plugins.configureRouting
import io.pcast.result.unwrap
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
            assertEquals(FEEDS.findAll().unwrap(::FeedResponse), body<List<FeedResponse>>())
        }
    }

    @Test
    fun testGetFeed() = testApplication {
        val client = configureServerAndGetClient()
        val feed = FEEDS.findAll().unwrap().first()
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
            assertEquals(HttpStatusCode.NotFound, status)
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

    @Test
    fun testUpdateFeed() = testApplication {
        val feed = FEEDS.findAll().unwrap().first()
        val newTitle = "new title"
        val client = configureServerAndGetClient()

        client.put("/api/feeds/${feed.id}") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(FeedRequest(newTitle, feed.url))
        }.apply {
            assertEquals(HttpStatusCode.NoContent, status)
        }

        client.get("/api/feeds/${feed.id}").apply {
            assertEquals(HttpStatusCode.OK, status)

            val response = body<FeedResponse>()

            assertEquals(newTitle, response.title)
        }
    }

    private fun ApplicationTestBuilder.configureServerAndGetClient(): HttpClient {
        application {
            install(ContentNegotiation) {
                json()
            }

            configureRouting()
        }

        return createClient {
            install(ClientContentNegotiation) {
                json()
            }
        }
    }
}
