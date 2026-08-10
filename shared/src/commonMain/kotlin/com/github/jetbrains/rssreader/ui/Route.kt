package com.github.jetbrains.rssreader.ui

import com.github.jetbrains.rssreader.Res
import com.github.jetbrains.rssreader.app_name
import com.github.jetbrains.rssreader.feed_list
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

sealed interface Route {
    val title: StringResource

    @Serializable
    data object Main : Route {
        override val title: StringResource get() = Res.string.app_name
    }

    @Serializable
    data object FeedList : Route {
        override val title: StringResource get() = Res.string.feed_list
    }
}
