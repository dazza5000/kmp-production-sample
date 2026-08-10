package com.github.jetbrains.rssreader.datasource.storage

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
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
