package com.tl.githubcompose.ui.popular

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tl.githubcompose.R
import com.tl.githubcompose.ui.components.ErrorRetry
import com.tl.githubcompose.ui.navigation.AppScreen
import com.tl.githubcompose.ui.profile.RepositoryCard

object PopularScreenTags {
    const val LOADING_INDICATOR = "PopularLoadingIndicator"
    const val REPO_LIST = "PopularRepoList"
    const val LOAD_MORE_INDICATOR = "PopularLoadMoreIndicator"
}

/**
 * Composable function for the Popular Repositories screen.
 * Displays a list of popular GitHub repositories fetched from the API,
 * supports pagination with pull-to-refresh and load-more.
 *
 * Note: This screen shares significant structural similarity with [SearchScreen].
 * Consider refactoring to extract a common paginated list component.
 *
 * @param navController The [NavController] used for navigating to repository details.
 * @param viewModel The [PopularReposViewModel] instance providing the UI state and data loading logic.
 */
@Composable
fun PopularReposScreen(
    viewModel: PopularReposViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading && !uiState.isLoadingMore)

    // Derived state to determine if more data should be loaded.
    // True when the last visible item is within 5 items of the end and not currently loading.
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 5 && !uiState.isLoading
        }
    }

    // Effect to trigger loading more data when shouldLoadMore becomes true.
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && uiState.hasMore) {
            viewModel.loadMorePopularRepos()
        }
    }

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.refreshPopularRepos() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && uiState.popularRepositories.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(PopularScreenTags.LOADING_INDICATOR)
                    )
                }
                uiState.error != null && uiState.popularRepositories.isEmpty() -> {
                    ErrorRetry(
                        message = stringResource(R.string.error_load_failed, uiState.error ?: ""),
                        onRetry = { viewModel.refreshPopularRepos() }
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(PopularScreenTags.REPO_LIST),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.popularRepositories) { repo ->
                            RepositoryCard(repository = repo, onClick = {
                                navController.navigate(AppScreen.Repository.createRoute(repo.owner.login, repo.name))
                            })
                        }

                        item {
                            if (uiState.isLoadingMore) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag(PopularScreenTags.LOAD_MORE_INDICATOR),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (uiState.error != null && uiState.popularRepositories.isNotEmpty()) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    Text(text = stringResource(R.string.error_load_failed, uiState.error ?: ""))
                }
            }
        }
    }
} 