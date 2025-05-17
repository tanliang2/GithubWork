package com.tl.githubcompose.ui.popular

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
 * Data class representing the UI state for the Popular Repositories screen.
 *
 * @param isLoading True if the initial load or a refresh is in progress.
 * @param isLoadingMore True if loading the next page of results is in progress.
 * @param popularRepositories The list of popular repositories currently displayed.
 * @param error An optional error message if loading failed.
 * @param hasMore True if there are potentially more results to load.
 * @param page The next page number to fetch for pagination.
 */
data class PopularReposUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val popularRepositories: List<Repository> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true,
    val page: Int = 1
)

/**
 * ViewModel for the Popular Repositories screen.
 * Fetches popular repositories from GitHub, manages pagination, and holds the UI state ([PopularReposUiState]).
 *
 * @param repository The [GithubRepository] used for fetching popular repositories.
 */
@HiltViewModel
class PopularReposViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PopularReposUiState(isLoading = true))
    /** StateFlow emitting the current [PopularReposUiState]. */
    val uiState: StateFlow<PopularReposUiState> = _uiState.asStateFlow()

    init {
        // Load initial data
        loadPopularRepos(isRefresh = false, isLoadMore = false)
    }

    /**
     * Internal function to load popular repositories.
     * Handles initial load, refresh, and load more scenarios based on parameters.
     *
     * @param isRefresh If true, clears existing data and loads page 1.
     * @param isLoadMore If true, loads the next page.
     */
    private fun loadPopularRepos(isRefresh: Boolean = false, isLoadMore: Boolean = false) {
        // Prevent loading more if already loading or no more pages exist
        if (isLoadMore && (!uiState.value.hasMore || uiState.value.isLoadingMore)) {
            return
        }

        // Determine the page to fetch
        val currentPage = if (isRefresh) 1 else uiState.value.page
        val perPage = 20 // Items per page

        viewModelScope.launch {
            // Update loading states based on the type of load
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, page = 1)
            } else if (isLoadMore) {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            } else {
                // Initial load
                _uiState.value = _uiState.value.copy(isLoading = true)
            }

            // Fetch data from repository
            repository.searchPopularRepos(page = currentPage, perPage = perPage)
                .collect { result ->
                    result.onSuccess { newRepos ->
                        // Determine whether to append or replace the list
                        val currentRepos = if (isRefresh || !isLoadMore) emptyList() else uiState.value.popularRepositories
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            popularRepositories = currentRepos + newRepos,
                            error = null,
                            // Assume more pages if we received a full page of results
                            hasMore = newRepos.size == perPage,
                            page = currentPage + 1
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

    /** Triggers a refresh of the popular repositories list. */
    fun refreshPopularRepos() {
        loadPopularRepos(isRefresh = true)
    }

    /** Triggers loading the next page of popular repositories. */
    fun loadMorePopularRepos() {
        loadPopularRepos(isLoadMore = true)
    }
} 