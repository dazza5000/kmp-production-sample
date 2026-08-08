package com.github.jetbrains.rssreader.presentation

import com.github.jetbrains.rssreader.core.wrap

fun FeedViewModel.watchState() = uiState.wrap()
