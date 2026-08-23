package com.github.jetbrains.rssreader.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.github.jetbrains.rssreader.core.RssReader

class FeedViewModel(
    private val rssReader: RssReader
) : ViewModel() {

    // UI STATE: lifecycle-scoped, retains across config changes
    private val _state = MutableStateFlow(
        FeedState(false, emptyList(), selectedFeed = null)
    )
    val state: StateFlow<FeedState> = _state.asStateFlow()

    // PUBLIC API: called from composables
    suspend fun loadFeeds(force: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(progress = true)
            try {
                val allFeeds = rssReader.getAllFeeds(force)
                _state.value = _state.value.copy(
                    progress = false,
                    feeds = allFeeds
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(progress = false)
            }
        }
    }

    suspend fun addFeed(url: String) {
        viewModelScope.launch {
            try {
                rssReader.addFeed(url)
                _state.value = _state.value.copy(progress = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(progress = false)
            }
        }
    }

    suspend fun deleteFeed(url: String) {
        viewModelScope.launch {
            try {
                rssReader.deleteFeed(url)
                _state.value = _state.value.copy(progress = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(progress = false)
            }
        }
    }

    suspend fun selectFeed() {
        _state.value = _state.value.copy(selectedFeed = null)
    }
}