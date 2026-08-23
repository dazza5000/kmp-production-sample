package com.github.jetbrains.rssreader.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jetbrains.rssreader.Res
import com.github.jetbrains.rssreader.back_button
import com.github.jetbrains.rssreader.presentation.FeedViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssFeedAppBar(
    title: StringResource,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(title)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_button)
                    )
                }
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScreen(
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(Unit) {
        viewModel.refresh(false)
    }
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refresh(true) },
        modifier = modifier,
        content = {
            MainFeed(
                uiState = state,
                onSelectFeed = { feed -> viewModel.selectFeed(feed) },
                onPostClick = { post ->
                    post.link?.let { url ->
                        uriHandler.openUri(url)
                    }
                },
                onEditClick = onEditClick
            )
        })
}

@Composable
fun FeedListScreen(
    viewModel: FeedViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeedList(
        uiState = state,
        onAddFeed = { url -> viewModel.addFeed(url) },
        onDeleteFeed = { url -> viewModel.deleteFeed(url) }
    )
}