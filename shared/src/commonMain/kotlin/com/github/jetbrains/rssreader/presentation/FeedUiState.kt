package com.github.jetbrains.rssreader.presentation

import com.github.jetbrains.rssreader.domain.Item
import com.github.jetbrains.rssreader.domain.RssFeed

data class FeedUiState(
    val isLoading: Boolean = false,
    val feeds: List<RssFeed> = emptyList(),
    val selectedFeed: RssFeed? = null
) {
    val mainFeedPosts: List<Item>
        get() = (selectedFeed?.channel?.item ?: feeds.flatMap { it.channel?.item ?: emptyList() })
            .sortedByDescending { it.pubDate }
}

sealed interface FeedUiEvent {
    data class ShowError(val message: String) : FeedUiEvent
}
