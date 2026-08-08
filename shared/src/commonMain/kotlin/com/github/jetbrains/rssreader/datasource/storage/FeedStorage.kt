package com.github.jetbrains.rssreader.datasource.storage

import com.github.jetbrains.rssreader.datasource.storage.entity.toDomain
import com.github.jetbrains.rssreader.datasource.storage.entity.toEntities
import com.github.jetbrains.rssreader.domain.RssFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedStorage(
    private val rssDao: RssDao
) {
    suspend fun getFeed(url: String): RssFeed? {
        return rssDao.getAllFeeds().firstOrNull { it.feed.url == url }?.toDomain()
    }

    suspend fun saveFeed(feed: RssFeed) {
        val (feedEntity, itemEntities) = feed.toEntities()
        rssDao.insertFeedWithItems(feedEntity, itemEntities)
    }

    suspend fun deleteFeed(url: String) {
        rssDao.deleteFeed(url)
    }

    suspend fun getAllFeeds(): List<RssFeed> {
        return rssDao.getAllFeeds().map { it.toDomain() }
    }

    fun observeAllFeeds(): Flow<List<RssFeed>> {
        return rssDao.observeAllFeeds().map { list -> list.map { it.toDomain() } }
    }
}