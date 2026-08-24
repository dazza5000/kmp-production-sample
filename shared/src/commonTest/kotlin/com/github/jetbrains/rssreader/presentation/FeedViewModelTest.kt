package com.github.jetbrains.rssreader.presentation

import com.github.jetbrains.rssreader.Settings
import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.datasource.storage.FakeRssDao
import com.github.jetbrains.rssreader.datasource.storage.FeedStorage
import com.github.jetbrains.rssreader.domain.Channel
import com.github.jetbrains.rssreader.domain.RssFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() {
        val storage = FeedStorage(FakeRssDao())
        val reader = RssReader(
            feedLoader = FeedLoader(io.ktor.client.HttpClient()),
            feedStorage = storage,
            settings = Settings(emptySet())
        )
        val viewModel = FeedViewModel(reader, testDispatcher)

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(emptyList(), state.feeds)
        assertNull(state.selectedFeed)
    }

    @Test
    fun testSelectFeed() {
        val storage = FeedStorage(FakeRssDao())
        val reader = RssReader(
            feedLoader = FeedLoader(io.ktor.client.HttpClient()),
            feedStorage = storage,
            settings = Settings(emptySet())
        )
        val viewModel = FeedViewModel(reader, testDispatcher)

        val feed = RssFeed(version = "2.0", sourceUrl = "https://example.com/rss", channel = Channel("Test", "Desc", "https://example.com", item = emptyList()))
        viewModel.selectFeed(feed)

        assertEquals(feed, viewModel.uiState.value.selectedFeed)
        assertEquals("Test", viewModel.uiState.value.selectedFeed?.channel?.title)
    }
}
