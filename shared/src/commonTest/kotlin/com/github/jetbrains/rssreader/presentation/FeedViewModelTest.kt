package com.github.jetbrains.rssreader.presentation

import com.github.jetbrains.rssreader.Settings
import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.datasource.storage.FeedStorage
import com.github.jetbrains.rssreader.domain.Channel
import com.github.jetbrains.rssreader.domain.RssFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class TestSettings : com.russhwolf.settings.Settings {
    private val map = mutableMapOf<String, Any>()
    override val keys: Set<String> get() = map.keys
    override val size: Int get() = map.size
    override fun hasKey(key: String): Boolean = map.containsKey(key)
    override fun clear() { map.clear() }
    override fun remove(key: String) { map.remove(key) }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
    override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
    override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = map[key] as? Float
    override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = map[key] as? Int
    override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = map[key] as? Long
    override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = map[key] as? String
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun putDouble(key: String, value: Double) { map[key] = value }
    override fun putFloat(key: String, value: Float) { map[key] = value }
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun putString(key: String, value: String) { map[key] = value }
}

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
        val storage = FeedStorage(TestSettings(), Json)
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
        val storage = FeedStorage(TestSettings(), Json)
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
