package com.tl.githubcompose.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tl.githubcompose.R
import com.tl.githubcompose.ui.components.ErrorRetry
import com.tl.githubcompose.ui.components.LanguageFilter
import com.tl.githubcompose.ui.components.RepoItem
import com.tl.githubcompose.ui.components.SearchBar
import com.tl.githubcompose.ui.navigation.AppScreen

/**
 * Composable function for the Search screen.
 * Allows users to search for GitHub repositories, filter by language,
 * view results in a paginated list with pull-to-refresh and load-more functionality.
 *
 * @param navController The [NavController] used for navigating to repository details.
 * @param viewModel The [SearchViewModel] instance providing the UI state and search logic.
 */
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading && !uiState.isLoadingMore)

    // Derived state to determine if more data should be loaded.
    // True when the last visible item is within 5 items of the end,
    // not currently loading, and the search query is not blank.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && 
            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 5 && 
            !uiState.isLoading && 
            uiState.searchQuery.isNotBlank()
        }
    }

    // Effect to trigger loading more data when shouldLoadMore becomes true.
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && uiState.hasMore) {
            viewModel.loadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.searchRepos() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            LanguageFilter(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelected = { 
                    viewModel.updateSelectedLanguage(it)
                    if (uiState.searchQuery.isNotBlank()) {
                        viewModel.searchRepos()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { 
                    if (uiState.searchQuery.isNotBlank()) {
                        viewModel.searchRepos() 
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading && uiState.repositories.isEmpty() -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        uiState.error != null && uiState.repositories.isEmpty() -> {
                            ErrorRetry(
                                message = stringResource(R.string.error_search_failed, uiState.error ?: ""),
                                onRetry = { viewModel.searchRepos() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.repositories) { repo ->
                                    RepoItem(
                                        repository = repo,
                                        onClick = {
                                            navController.navigate(AppScreen.Repository.createRoute(repo.owner.login, repo.name))
                                        }
                                    )
                                }

                                item {
                                    if (uiState.isLoadingMore) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.error != null && uiState.repositories.isNotEmpty()) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = stringResource(R.string.error_load_failed, uiState.error ?: stringResource(R.string.error_unknown)))
            }
        }
    }
} 