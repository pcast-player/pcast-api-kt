package io.pcast

import com.fasterxml.uuid.Generators
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.pcast.model.FakeFeedRepository
import io.pcast.model.Feed
import io.pcast.plugins.configureFeed
import kotlin.test.Test
import kotlin.test.assertEquals

private val FEEDS = FakeFeedRepository()

internal class FeedsTest {
    @Test
    fun testGetFeeds() = testApplication {
        val client = configureServerAndGetClient()

        client.get("/api/feeds").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(FEEDS.findAll(), body<List<Feed>>())
        }
    }

    @Test
    fun testGetFeed() = testApplication {
        val client = configureServerAndGetClient()
        val feed = FEEDS.findAll().first()

        client.get("/api/feeds/${feed.id}").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(feed, body<Feed>())
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
