package com.tl.githubcompose.ui.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tl.githubcompose.data.GithubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for handling the creation of new issues.
 *
 * @param repository The [GithubRepository] used for creating issues.
 */
@HiltViewModel
class IssueViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssueUiState())
    /** StateFlow emitting the current [IssueUiState]. */
    val uiState: StateFlow<IssueUiState> = _uiState.asStateFlow()

    /**
     * Attempts to create a new issue in the specified repository.
     * Updates the loading state in [uiState] and invokes callbacks on completion.
     *
     * @param owner The repository owner.
     * @param repo The repository name.
     * @param title The title of the new issue.
     * @param body The optional body content of the new issue.
     * @param onSuccess Callback invoked with the new issue number upon successful creation.
     * @param onError Callback invoked with an error message upon failure.
     */
    fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String?,
        onSuccess: (issueNumber: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.createIssue(owner, repo, title, body)
                .collect { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    result.fold(
                        onSuccess = { issue ->
                            onSuccess(issue.number)
                        },
                        onFailure = { error ->
                            onError(error.message ?: "Failed to create issue")
                        }
                    )
                }
        }
    }
}

/**
 * Data class representing the UI state specifically for the issue creation process.
 *
 * @param isLoading True if the issue creation request is in progress.
 */
data class IssueUiState(
    val isLoading: Boolean = false
) 