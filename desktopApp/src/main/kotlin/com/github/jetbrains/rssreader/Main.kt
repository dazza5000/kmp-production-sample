package com.github.jetbrains.rssreader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.room3.Room
import com.github.jetbrains.rssreader.core.HttpClient
import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.datasource.storage.FeedStorage
import com.github.jetbrains.rssreader.datasource.storage.RssDatabase
import com.github.jetbrains.rssreader.datasource.storage.getRoomDatabase
import com.github.jetbrains.rssreader.presentation.FeedViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File

private val appModule = module {
    single {
        val dbFile = File(System.getProperty("user.home"), ".rssreader/rss_reader.db")
        dbFile.parentFile?.mkdirs()
        val builder = Room.databaseBuilder<RssDatabase>(
            name = dbFile.absolutePath
        )
        getRoomDatabase(builder)
    }
    single { FeedStorage(get<RssDatabase>().rssDao()) }
    single { RssReader(get(), get(), Settings(setOf("https://blog.jetbrains.com/kotlin/feed/"))) }
    factory { FeedViewModel(get()) }
    single { FeedLoader(get()) }
    single { HttpClient(false) }
}

private fun initKoin() {
    startKoin {
        modules(appModule)
    }
}

fun main() = application {
    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "RSS reader",
    ) {
        RssReaderApp()
    }
}