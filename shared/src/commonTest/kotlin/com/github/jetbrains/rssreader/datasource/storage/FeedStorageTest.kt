package com.github.jetbrains.rssreader.datasource.storage

import com.github.jetbrains.rssreader.datasource.storage.entity.FeedWithItems
import com.github.jetbrains.rssreader.domain.Channel
import com.github.jetbrains.rssreader.domain.Image
import com.github.jetbrains.rssreader.domain.Item
import com.github.jetbrains.rssreader.domain.MediaContent
import com.github.jetbrains.rssreader.domain.RssFeed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedStorageTest {

    private fun createSampleFeed(
        url: String = "https://example.com/feed.xml",
        isDefault: Boolean = false,
        itemCount: Int = 2
    ): RssFeed {
        val items = (1..itemCount).map { i ->
            Item(
                title = "Item $i",
                pubDate = "Mon, 10 Aug 2026 08:00:0$i +0000",
                link = "https://example.com/items/$i",
                guid = "guid-$i",
                description = "Description for item $i",
                contentEncoded = "<p>Content $i</p>",
                mediaContent = MediaContent(url = "https://example.com/media/$i.jpg")
            )
        }
        return RssFeed(
            version = "2.0",
            sourceUrl = url,
            isDefault = isDefault,
            channel = Channel(
                title = "Sample Channel",
                description = "Sample Channel Description",
                link = "https://example.com",
                copyright = "2026 Sample",
                item = items,
                image = Image(
                    url = "https://example.com/image.png",
                    title = null,
                    link = null,
                    width = null,
                    height = null
                )
            )
        )
    }

    @Test
    fun testSaveAndGetAllFeedsRoundTrip() = runTest {
        val dao = FakeRssDao()
        val storage = FeedStorage(dao)

        val feed = createSampleFeed(isDefault = true, itemCount = 2)
        storage.saveFeed(feed)

        val storedFeeds = storage.getAllFeeds()
        assertEquals(1, storedFeeds.size)

        val storedFeed = storedFeeds.first()
        assertEquals("https://example.com/feed.xml", storedFeed.sourceUrl)
        assertEquals("2.0", storedFeed.version)
        assertTrue(storedFeed.isDefault)

        val channel = storedFeed.channel
        assertNotNull(channel)
        assertEquals("Sample Channel", channel.title)
        assertEquals("Sample Channel Description", channel.description)
        assertEquals("https://example.com", channel.link)
        assertEquals("2026 Sample", channel.copyright)
        assertEquals("https://example.com/image.png", channel.image?.url)

        val items = channel.item
        assertEquals(2, items.size)

        val item1 = items[0]
        assertEquals("Item 1", item1.title)
        assertEquals("Mon, 10 Aug 2026 08:00:01 +0000", item1.pubDate)
        assertEquals("https://example.com/items/1", item1.link)
        assertEquals("guid-1", item1.guid)
        assertEquals("Description for item 1", item1.description)
        assertEquals("<p>Content 1</p>", item1.contentEncoded)
        assertEquals("https://example.com/media/1.jpg", item1.mediaContent?.url)
    }

    @Test
    fun testSaveFeedTwiceReplacesAndDoesNotDuplicateItems() = runTest {
        val dao = FakeRssDao()
        val storage = FeedStorage(dao)

        val feed1 = createSampleFeed(url = "https://example.com/feed.xml", itemCount = 2)
        storage.saveFeed(feed1)

        val feed2 = createSampleFeed(url = "https://example.com/feed.xml", itemCount = 2)
        storage.saveFeed(feed2)

        val storedFeeds = storage.getAllFeeds()
        assertEquals(1, storedFeeds.size)
        assertEquals(2, storedFeeds.first().channel?.item?.size)
    }

    @Test
    fun testDeleteFeed() = runTest {
        val dao = FakeRssDao()
        val storage = FeedStorage(dao)

        val feed = createSampleFeed(url = "https://example.com/feed.xml")
        storage.saveFeed(feed)
        assertEquals(1, storage.getAllFeeds().size)

        storage.deleteFeed("https://example.com/feed.xml")
        assertTrue(storage.getAllFeeds().isEmpty())
    }

    @Test
    fun testGetFeed() = runTest {
        val dao = FakeRssDao()
        val storage = FeedStorage(dao)

        val feed = createSampleFeed(url = "https://example.com/feed.xml")
        storage.saveFeed(feed)

        val found = storage.getFeed("https://example.com/feed.xml")
        assertNotNull(found)
        assertEquals("https://example.com/feed.xml", found.sourceUrl)
        assertEquals("Sample Channel", found.channel?.title)

        val notFound = storage.getFeed("https://unknown.com/feed.xml")
        assertNull(notFound)
    }

    @Test
    fun testObserveAllFeeds() = runTest {
        val dao = FakeRssDao()
        val storage = FeedStorage(dao)

        val emissions = mutableListOf<List<RssFeed>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            storage.observeAllFeeds().collect { emissions.add(it) }
        }

        // Initial emission should be empty
        assertEquals(1, emissions.size)
        assertTrue(emissions[0].isEmpty())

        // Save feed -> should emit updated list
        val feed = createSampleFeed(url = "https://example.com/feed.xml")
        storage.saveFeed(feed)

        assertEquals(2, emissions.size)
        assertEquals(1, emissions[1].size)
        assertEquals("https://example.com/feed.xml", emissions[1][0].sourceUrl)

        job.cancel()
    }

    @Test
    fun testEntityDomainMappingPreservesIsDefault() {
        val defaultFeed = createSampleFeed(isDefault = true)
        val (defaultEntity, defaultItems) = FeedWithItems.fromDomain(defaultFeed)
        assertTrue(defaultEntity.isDefault)
        val defaultMappedBack = FeedWithItems(defaultEntity, defaultItems).toDomain()
        assertTrue(defaultMappedBack.isDefault)

        val nonDefaultFeed = createSampleFeed(isDefault = false)
        val (nonDefaultEntity, nonDefaultItems) = FeedWithItems.fromDomain(nonDefaultFeed)
        assertFalse(nonDefaultEntity.isDefault)
        val nonDefaultMappedBack = FeedWithItems(nonDefaultEntity, nonDefaultItems).toDomain()
        assertFalse(nonDefaultMappedBack.isDefault)
    }
}
