package com.github.jetbrains.rssreader.core

import android.content.Context
import androidx.room.Room
import com.github.jetbrains.rssreader.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.datasource.storage.FeedStorage
import com.github.jetbrains.rssreader.datasource.storage.RssDatabase
import com.github.jetbrains.rssreader.datasource.storage.getRoomDatabase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun buildRssReader(ctx: Context, withLog: Boolean): RssReader {
    val dbFile = ctx.applicationContext.getDatabasePath("rss_reader.db")
    val builder = Room.databaseBuilder<RssDatabase>(
        context = ctx.applicationContext,
        name = dbFile.absolutePath
    )
    val rssDatabase = getRoomDatabase(builder)

    return RssReader(
        FeedLoader(HttpClient(withLog)),
        FeedStorage(rssDatabase.rssDao())
    ).also {
        if (withLog) Napier.base(DebugAntilog())
    }
}