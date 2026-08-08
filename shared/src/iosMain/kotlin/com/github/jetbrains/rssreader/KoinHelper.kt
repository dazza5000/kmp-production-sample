package com.github.jetbrains.rssreader

import androidx.room.Room
import com.github.jetbrains.rssreader.core.HttpClient
import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.datasource.storage.FeedStorage
import com.github.jetbrains.rssreader.datasource.storage.RssDatabase
import com.github.jetbrains.rssreader.datasource.storage.getRoomDatabase
import com.github.jetbrains.rssreader.presentation.FeedViewModel
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private val appModule = module {
    single {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        val dbFile = requireNotNull(documentDirectory?.path) + "/rss_reader.db"
        val builder = Room.databaseBuilder<RssDatabase>(
            name = dbFile
        )
        getRoomDatabase(builder)
    }
    single { FeedStorage(get<RssDatabase>().rssDao()) }
    single { RssReader(get(), get(), Settings(setOf("https://blog.jetbrains.com/kotlin/feed/"))) }
    factory { FeedViewModel(get()) }
    single { FeedLoader(get()) }
    single { HttpClient(false) }
}

class KoinHelper : KoinComponent {
    val rssReader by inject<RssReader>()
    val feedViewModel by inject<FeedViewModel>()
}

fun initKoin() {
    startKoin { modules(appModule) }
}