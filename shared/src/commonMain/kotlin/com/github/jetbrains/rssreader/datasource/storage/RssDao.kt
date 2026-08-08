package com.github.jetbrains.rssreader.datasource.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.github.jetbrains.rssreader.datasource.storage.entity.FeedEntity
import com.github.jetbrains.rssreader.datasource.storage.entity.FeedWithItems
import com.github.jetbrains.rssreader.datasource.storage.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RssDao {
    @Transaction
    @Query("SELECT * FROM feeds")
    fun observeAllFeeds(): Flow<List<FeedWithItems>>

    @Transaction
    @Query("SELECT * FROM feeds")
    suspend fun getAllFeeds(): List<FeedWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: FeedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Transaction
    suspend fun insertFeedWithItems(feed: FeedEntity, items: List<ItemEntity>) {
        insertFeed(feed)
        insertItems(items)
    }

    @Query("DELETE FROM feeds WHERE url = :url")
    suspend fun deleteFeed(url: String)
}
