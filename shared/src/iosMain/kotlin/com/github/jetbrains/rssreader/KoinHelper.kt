package com.github.jetbrains.rssreader

import com.github.jetbrains.rssreader.presentation.FeedViewModel
import com.github.jetbrains.rssreader.core.HttpClient
import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.datasource.storage.FeedStorage
import com.russhwolf.settings.NSUserDefaultsSettings
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

private val appModule = module {
    single { RssReader(get(), get(), Settings(setOf("https://blog.jetbrains.com/kotlin/feed/"))) }
    single<FeedStorage> {
        FeedStorage(
            NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults()),
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            }
        )
    }
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