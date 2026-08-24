package com.github.jetbrains.rssreader.core

import com.github.jetbrains.rssreader.Settings
import com.github.jetbrains.rssreader.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.datasource.storage.FakeRssDao
import com.github.jetbrains.rssreader.datasource.storage.FeedStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.coroutines.test.runTest
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalXmlUtilApi::class)
class RssReaderTest {

    private val sampleRssXml = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
    <channel>
        <title>Sample Feed</title>
        <description>Sample Description</description>
        <link>https://example.com</link>
        <item>
            <title>Sample Item</title>
            <pubDate>Mon, 10 Aug 2026 08:00:00 +0000</pubDate>
            <link>https://example.com/item/1</link>
            <guid>sample-1</guid>
            <description>Item Description</description>
        </item>
    </channel>
</rss>"""

    private fun createMockHttpClient(onHandleRequest: () -> Unit = {}): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    onHandleRequest()
                    respond(
                        content = sampleRssXml,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Rss.toString())
                    )
                }
            }
            install(ContentNegotiation) {
                xml(contentType = ContentType.Application.Rss, format = XML {
                    defaultPolicy {
                        unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
                    }
                })
            }
        }
    }

    @Test
    fun testGetAllFeedsWithEmptyStorageFetchesDefaultsAndCaches() = runTest {
        var networkHits = 0
        val httpClient = createMockHttpClient { networkHits++ }
        val feedLoader = FeedLoader(httpClient)
        val storage = FeedStorage(FakeRssDao())
        val defaultUrl = "https://blog.jetbrains.com/kotlin/feed/"
        val settings = Settings(setOf(defaultUrl))
        val reader = RssReader(feedLoader, storage, settings)

        // First call: empty storage, should fetch defaults from network
        val initialFeeds = reader.getAllFeeds(forceUpdate = false)
        assertEquals(1, initialFeeds.size)
        assertEquals(1, networkHits)
        assertTrue(initialFeeds.first().isDefault)
        assertEquals(defaultUrl, initialFeeds.first().sourceUrl)

        // Second call: without forceUpdate, serves from storage without network hit
        val cachedFeeds = reader.getAllFeeds(forceUpdate = false)
        assertEquals(1, cachedFeeds.size)
        assertEquals(1, networkHits)
    }

    @Test
    fun testGetAllFeedsWithForceUpdateRefetchesAllStored() = runTest {
        var networkHits = 0
        val httpClient = createMockHttpClient { networkHits++ }
        val feedLoader = FeedLoader(httpClient)
        val storage = FeedStorage(FakeRssDao())
        val defaultUrl = "https://blog.jetbrains.com/kotlin/feed/"
        val settings = Settings(setOf(defaultUrl))
        val reader = RssReader(feedLoader, storage, settings)

        // Initial load
        reader.getAllFeeds(forceUpdate = false)
        assertEquals(1, networkHits)

        // Force update: should hit network for each stored feed
        val refreshed = reader.getAllFeeds(forceUpdate = true)
        assertEquals(1, refreshed.size)
        assertEquals(2, networkHits)
    }

    @Test
    fun testAddAndDeleteFeed() = runTest {
        var networkHits = 0
        val httpClient = createMockHttpClient { networkHits++ }
        val feedLoader = FeedLoader(httpClient)
        val storage = FeedStorage(FakeRssDao())
        val settings = Settings(setOf("https://default.com/feed"))
        val reader = RssReader(feedLoader, storage, settings)

        val customUrl = "https://custom.com/feed"
        reader.addFeed(customUrl)
        assertEquals(1, networkHits)

        val feedsAfterAdd = reader.getAllFeeds(forceUpdate = false)
        assertEquals(1, feedsAfterAdd.size)
        assertEquals(customUrl, feedsAfterAdd.first().sourceUrl)
        assertFalse(feedsAfterAdd.first().isDefault)

        reader.deleteFeed(customUrl)
        val feedsAfterDelete = storage.getAllFeeds()
        assertTrue(feedsAfterDelete.isEmpty())
    }

    @Test
    fun testIsDefaultFlagDistinction() = runTest {
        val httpClient = createMockHttpClient()
        val feedLoader = FeedLoader(httpClient)
        val storage = FeedStorage(FakeRssDao())
        val defaultUrl = "https://default.com/feed"
        val customUrl = "https://custom.com/feed"
        val settings = Settings(setOf(defaultUrl))
        val reader = RssReader(feedLoader, storage, settings)

        // Fetch default feed
        val defaultFeeds = reader.getAllFeeds(forceUpdate = false)
        assertTrue(defaultFeeds.first().isDefault)

        // Add custom feed
        reader.addFeed(customUrl)
        val allFeeds = storage.getAllFeeds()
        val custom = allFeeds.first { it.sourceUrl == customUrl }
        val defaultFeed = allFeeds.first { it.sourceUrl == defaultUrl }

        assertTrue(defaultFeed.isDefault)
        assertFalse(custom.isDefault)
    }
}
