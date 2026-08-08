package com.github.jetbrains.rssreader.datasource.storage

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.github.jetbrains.rssreader.datasource.storage.entity.FeedEntity
import com.github.jetbrains.rssreader.datasource.storage.entity.ItemEntity

@Database(entities = [FeedEntity::class, ItemEntity::class], version = 1)
@ConstructedBy(RssDatabaseConstructor::class)
abstract class RssDatabase : RoomDatabase() {
    abstract fun rssDao(): RssDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object RssDatabaseConstructor : RoomDatabaseConstructor<RssDatabase> {
    override fun initialize(): RssDatabase
}
