package com.tl.githubcompose.ui.issue

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
 * Data class representing the UI state for the Issue Detail screen.
 *
 * @param isLoading True if loading the issue details is in progress.
 * @param problem The loaded [Problem] object (null if not loaded or error).
 * @param error An optional error message if loading failed.
 */
data class IssueDetailUiState(
    val isLoading: Boolean = false,
    val problem: Problem? = null,
    val error: String? = null
)

/**
 * ViewModel for the Issue Detail screen.
 * Fetches the details for a specific issue.
 *
 * Note: Uses `repository.getIssueDetail` which throws an exception on failure,
 * handled by try-catch here. Consider refactoring repository layer for consistency.
 *
 * @param repository The [GithubRepository] used for fetching issue details.
 */
@HiltViewModel
class IssueDetailViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssueDetailUiState())
    /** StateFlow emitting the current [IssueDetailUiState]. */
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()

    /**
     * Loads the details for the specified issue.
     *
     * @param owner The repository owner.
     * @param repoName The repository name.
     * @param issueNumber The number of the issue to load.
     */
    fun loadIssueDetail(owner: String, repoName: String, issueNumber: Int) {
        viewModelScope.launch {
            _uiState.value = IssueDetailUiState(isLoading = true) // Reset state on load
            try {
                // Fetch issue details (repository method throws on error)
                val issue = repository.getIssueDetail(owner, repoName, issueNumber)
                // Success: Update state with issue data
                _uiState.value = IssueDetailUiState(
                    isLoading = false,
                    problem = issue,
                    error = null
                )
            } catch (e: Exception) {
                // Failure: Update state with error message
                Log.e("IssueDetailViewModel", "Failed to load issue detail", e)
                _uiState.value = IssueDetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load issue details" // English error
                )
                // TODO: Consider moving error strings to resources for localization.
            }
        }
    }
} 