package com.github.jetbrains.rssreader.datasource.storage

import com.github.jetbrains.rssreader.datasource.storage.entity.FeedEntity
import com.github.jetbrains.rssreader.datasource.storage.entity.FeedWithItems
import com.github.jetbrains.rssreader.datasource.storage.entity.ItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRssDao : RssDao {
    private val feedsMap = mutableMapOf<String, FeedEntity>()
    private val itemsMap = mutableMapOf<String, MutableList<ItemEntity>>()
    private val flow = MutableStateFlow<List<FeedWithItems>>(emptyList())

    private fun emit() {
        val list = feedsMap.values.map { feed ->
            FeedWithItems(feed, itemsMap[feed.url] ?: emptyList())
        }
        flow.value = list
    }

    override fun observeAllFeeds(): Flow<List<FeedWithItems>> = flow

    override suspend fun getAllFeeds(): List<FeedWithItems> {
        return feedsMap.values.map { feed ->
            FeedWithItems(feed, itemsMap[feed.url] ?: emptyList())
        }
    }

    override suspend fun insertFeed(feed: FeedEntity) {
        feedsMap[feed.url] = feed
        itemsMap.remove(feed.url)
        emit()
    }

    override suspend fun insertItems(items: List<ItemEntity>) {
        items.forEach { item ->
            itemsMap.getOrPut(item.feedUrl) { mutableListOf() }.add(item)
        }
        emit()
    }

    override suspend fun insertFeedWithItems(feed: FeedEntity, items: List<ItemEntity>) {
        feedsMap[feed.url] = feed
        itemsMap[feed.url] = items.toMutableList()
        emit()
    }

    override suspend fun deleteFeed(url: String) {
        feedsMap.remove(url)
        itemsMap.remove(url)
        emit()
    }
}
