package com.github.jetbrains.rssreader.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.domain.RssFeed
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(
    private val rssReader: RssReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<FeedUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<FeedUiEvent> = _uiEvent.receiveAsFlow()

    fun refresh(forceLoad: Boolean = false) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val feeds = rssReader.getAllFeeds(forceLoad)
                _uiState.update { state ->
                    val selected = state.selectedFeed?.let { sel ->
                        if (feeds.contains(sel)) sel else null
                    }
                    state.copy(isLoading = false, feeds = feeds, selectedFeed = selected)
                }
            } catch (e: Exception) {
                Napier.e("Error loading feeds", e)
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.send(FeedUiEvent.ShowError(e.message ?: "Failed to refresh feeds"))
            }
        }
    }

    fun addFeed(url: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                rssReader.addFeed(url)
                val feeds = rssReader.getAllFeeds(false)
                _uiState.update { it.copy(isLoading = false, feeds = feeds) }
            } catch (e: Exception) {
                Napier.e("Error adding feed: $url", e)
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.send(FeedUiEvent.ShowError(e.message ?: "Failed to add feed"))
            }
        }
    }

    fun deleteFeed(url: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                rssReader.deleteFeed(url)
                val feeds = rssReader.getAllFeeds(false)
                _uiState.update { it.copy(isLoading = false, feeds = feeds) }
            } catch (e: Exception) {
                Napier.e("Error deleting feed: $url", e)
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.send(FeedUiEvent.ShowError(e.message ?: "Failed to delete feed"))
            }
        }
    }

    fun selectFeed(feed: RssFeed?) {
        _uiState.update { it.copy(selectedFeed = feed) }
    }
}
