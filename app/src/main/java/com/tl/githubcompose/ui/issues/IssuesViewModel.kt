package com.tl.githubcompose.ui.issues

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tl.githubcompose.data.GithubRepository
import com.tl.githubcompose.data.model.Problem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing the UI state for the Issues list screen.
 *
 * @param isLoading True if the initial load is in progress.
 * @param isLoadingMore True if loading the next page of results is in progress.
 * @param isRefreshing True if a refresh operation is in progress.
 * @param problems The list of issues currently displayed.
 * @param error An optional error message if loading failed.
 * @param hasMore True if there are potentially more results to load.
 * @param currentPage The next page number to fetch for pagination.
 */
data class IssuesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val problems: List<Problem> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 1
)

/**
 * ViewModel for the Issues list screen.
 * Fetches issues for a specific repository, manages pagination, and holds the UI state ([IssuesUiState]).
 *
 * Note: This ViewModel uses a different data loading pattern (`repository.getIssues` throwing exceptions)
 * compared to other ViewModels using Flow<Result>. Consider refactoring for consistency.
 * Note: Loading state management (isLoading, isLoadingMore, isRefreshing) could potentially be simplified.
 *
 * @param repository The [GithubRepository] used for fetching issues.
 */
@HiltViewModel
class IssuesViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssuesUiState())
    /** StateFlow emitting the current [IssuesUiState]. */
    val uiState: StateFlow<IssuesUiState> = _uiState.asStateFlow()
    
    private val pageSize = 20 // Number of issues to fetch per page

    /**
     * Loads issues for the specified repository.
     * Handles initial load, refresh, and load more scenarios.
     *
     * @param owner The repository owner.
     * @param repoName The repository name.
     * @param isRefresh True if this is a refresh operation, false otherwise.
     */
    fun loadIssues(owner: String, repoName: String, isRefresh: Boolean = false) {
        val currentState = _uiState.value

        // Determine the type of load and update state accordingly
        if (isRefresh) {
            // Prevent concurrent refresh
            if (currentState.isRefreshing) return
            _uiState.value = currentState.copy(
                isRefreshing = true,
                currentPage = 1,
                hasMore = true // Assume has more on refresh
            )
        } else {
            // Prevent loading more if already loading, no more pages, or initial load is happening
            if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMore) {
                return
            }
            // Set loading state based on whether it's the first load or loading more
            _uiState.value = currentState.copy(
                isLoading = currentState.problems.isEmpty(), // True only if list is currently empty
                isLoadingMore = currentState.problems.isNotEmpty() // True only if list is not empty
            )
        }

        viewModelScope.launch {
            try {
                // Page to fetch (reset to 1 on refresh)
                val pageToFetch = if (isRefresh) 1 else _uiState.value.currentPage
                // Fetch issues (Note: repository.getIssues throws exception on failure)
                val newIssues = repository.getIssues(owner, repoName, pageToFetch, pageSize)

                // Success: Update state
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    isRefreshing = false,
                    // Replace list on refresh, append otherwise
                    problems = if (isRefresh) newIssues else _uiState.value.problems + newIssues,
                    error = null, // Clear error on success
                    // Assume more pages if we received a full page
                    hasMore = newIssues.size >= pageSize,
                    currentPage = pageToFetch + 1
                )
            } catch (e: Exception) {
                // Failure: Update state with error
                Log.e("IssuesViewModel", "Failed to load issues", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load issues", // Use English error message
                    // Optional: Could set hasMore to false on error, or let user retry
                    // hasMore = false
                )
                // TODO: Consider moving error strings to resources for localization.
            }
        }
    }

    /**
     * Loads the next page of issues.
     *
     * @param owner The repository owner.
     * @param repoName The repository name.
     */
    fun loadMore(owner: String, repoName: String) {
        // Call loadIssues, ensuring it's not treated as a refresh
        loadIssues(owner, repoName, isRefresh = false)
    }

    /**
     * Refreshes the list of issues.
     *
     * @param owner The repository owner.
     * @param repoName The repository name.
     */
    fun refresh(owner: String, repoName: String) {
        loadIssues(owner, repoName, isRefresh = true)
    }
} 