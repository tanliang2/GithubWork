package com.tl.githubcompose.ui.issues

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tl.githubcompose.R
import com.tl.githubcompose.data.model.Problem
import com.tl.githubcompose.ui.components.ErrorRetry

/**
 * Composable function for the Issues list screen.
 * Displays a list of issues for a specific repository, supports pagination and pull-to-refresh.
 *
 * Note: This screen shares structural similarity with other list screens ([SearchScreen], etc.).
 * Consider refactoring to extract a common paginated list component.
 *
 * @param owner The repository owner.
 * @param repoName The repository name.
 * @param viewModel The [IssuesViewModel] providing UI state and data loading logic.
 * @param onNavigateBack Callback invoked when the back navigation action is triggered.
 * @param onNavigateToDetail Callback invoked when an issue item is clicked, providing details needed for navigation.
 */
@Composable
fun IssuesScreen(
    owner: String,
    repoName: String,
    viewModel: IssuesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (owner: String, repoName: String, issueNumber: Int) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)

    // Listen for scroll reaching the end to trigger loading more
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            // Using -5 threshold for consistency
            lastVisibleItem?.index != null &&
            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 5 &&
            !uiState.isLoading &&
            !uiState.isLoadingMore &&
            uiState.hasMore
        }
    }

    // Trigger loading more when conditions are met
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore(owner, repoName)
        }
    }

    // Trigger initial load if needed (e.g., on first composition or if owner/repo changes)
    LaunchedEffect(owner, repoName) {
        // Only trigger initial load if list is empty and not already loading
        if (uiState.problems.isEmpty() && !uiState.isLoading && !uiState.isLoadingMore && !uiState.isRefreshing) {
            viewModel.loadIssues(owner, repoName)
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.issues_title), color = Color.Black) },
                backgroundColor = Color.White,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back),
                            tint = Color.Black
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refresh(owner, repoName) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    // Initial error state
                    uiState.error != null && uiState.problems.isEmpty() -> {
                        ErrorRetry(
                            message = uiState.error ?: stringResource(id = R.string.error_unknown),
                            onRetry = { viewModel.loadIssues(owner, repoName) }
                        )
                    }
                    // Empty state (loaded, but no issues)
                    uiState.problems.isEmpty() && !uiState.isLoading && !uiState.isRefreshing -> {
                        Text(
                            text = stringResource(id = R.string.issues_none_found),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.problems) { issue ->
                                IssueItem(
                                    problem = issue,
                                    onClick = { onNavigateToDetail(owner, repoName, issue.number) }
                                )
                            }

                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
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

                        // Snackbar for errors when data is already present
                        if (uiState.error != null) {
                            Snackbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            ) {
                                Text(text = stringResource(id = R.string.error_prefix_detail, uiState.error ?: ""))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable function displaying a single issue item in a Card.
 *
 * @param problem The [Problem] data to display.
 * @param onClick Callback invoked when the card is clicked.
 */
@Composable
fun IssueItem(
    problem: Problem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = problem.title,
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.issue_item_subtitle, problem.number, problem.user.login),
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
        }
    }
} 