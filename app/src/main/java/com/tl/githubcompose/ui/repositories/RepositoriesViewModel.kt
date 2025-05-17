package com.tl.githubcompose.ui.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tl.githubcompose.data.GithubRepository
import com.tl.githubcompose.data.model.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing the UI state for the user's Repositories screen.
 *
 * @param isLoading True if the initial load or a refresh is in progress.
 * @param isLoadingMore True if loading the next page of results is in progress.
 * @param repositories The list of the user's repositories currently displayed.
 * @param error An optional error message if loading failed.
 * @param hasMore True if there are potentially more results to load.
 * @param page The next page number to fetch for pagination.
 */
data class RepositoriesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val repositories: List<Repository> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true,
    val page: Int = 1
)

/**
 * ViewModel for the screen displaying the authenticated user's repositories.
 * Fetches the user's repositories from GitHub, manages pagination, and holds the UI state ([RepositoriesUiState]).
 *
 * Note: This ViewModel shares significant structural similarity with [SearchViewModel] and [PopularReposViewModel].
 * Consider refactoring to extract a common pagination logic.
 *
 * @param repository The [GithubRepository] used for fetching user repositories.
 */
@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoriesUiState(isLoading = false))
    /** StateFlow emitting the current [RepositoriesUiState]. */
    val uiState: StateFlow<RepositoriesUiState> = _uiState.asStateFlow()

    init {
        // Load initial data when the ViewModel is created
        loadRepositories()
    }

    /**
     * Internal function to load the user's repositories.
     * Handles initial load, refresh, and load more scenarios.
     *
     * @param isLoadMore If true, loads the next page.
     */
    private fun loadRepositories(isLoadMore: Boolean = false) {
        // Prevent concurrent loads or loading more when no more pages exist
        if ((isLoadMore && (!uiState.value.hasMore || uiState.value.isLoadingMore)) || (!isLoadMore && uiState.value.isLoading)) {
            return
        }

        val currentPage = if (isLoadMore) uiState.value.page else 1
        val perPage = 20 // Items per page

        viewModelScope.launch {
            // Update loading state
            _uiState.value = if (isLoadMore) {
                _uiState.value.copy(isLoadingMore = true)
            } else {
                // Reset error and set loading for initial/refresh
                _uiState.value.copy(isLoading = true, error = null, page = 1)
            }

            // Fetch data from repository
            // Consider using a dedicated /user/repos endpoint if available in repository
            repository.getUserRepos(page = currentPage, perPage = perPage)
                .collect { result ->
                    result.onSuccess { newRepos ->
                        val currentRepos = if (isLoadMore) uiState.value.repositories else emptyList()
                        // Determine the next page number correctly
                        val nextPage = currentPage + 1
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            repositories = currentRepos + newRepos,
                            error = null,
                            // Assume more pages if we received a full page of results
                            hasMore = newRepos.size == perPage,
                            page = nextPage
                        )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    /** Triggers a refresh of the repositories list. */
    fun refresh() {
        // Call loadRepositories ensuring it's treated as a refresh (page 1)
        loadRepositories(isLoadMore = false)
    }

    /** Triggers loading the next page of repositories. */
    fun loadMore() {
        loadRepositories(isLoadMore = true)
    }
}