package com.github.jetbrains.rssreader

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jetbrains.rssreader.presentation.FeedUiEvent
import com.github.jetbrains.rssreader.presentation.FeedViewModel
import com.github.jetbrains.rssreader.ui.AppTheme
import com.github.jetbrains.rssreader.ui.FeedListScreen
import com.github.jetbrains.rssreader.ui.MainScreen
import com.github.jetbrains.rssreader.ui.Route
import com.github.jetbrains.rssreader.ui.RssFeedAppBar
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssReaderApp(navController: NavHostController = rememberNavController()) {
    AppTheme {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentTitle = when {
            backStackEntry?.destination?.hasRoute<Route.FeedList>() == true -> Route.FeedList.title
            else -> Route.Main.title
        }
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                RssFeedAppBar(
                    title = currentTitle,
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navigateUp = { navController.navigateUp() }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    modifier = Modifier.padding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues()
                    ), hostState = snackbarHostState
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Main,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable<Route.Main> {
                    MainScreen(
                        onEditClick = { navController.navigate(Route.FeedList) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                composable<Route.FeedList> {
                    FeedListScreen()
                }
            }

            val viewModel: FeedViewModel = koinViewModel()
            LaunchedEffect(Unit) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is FeedUiEvent.ShowError -> {
                            snackbarHostState.showSnackbar(event.message)
                        }
                    }
                }
            }
        }
    }
}