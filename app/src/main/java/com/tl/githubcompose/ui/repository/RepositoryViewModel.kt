package com.tl.githubcompose.ui.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tl.githubcompose.data.GithubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Repository Detail screen.
 * Fetches repository details and README content concurrently for a given repository.
 *
 * @param repository The [GithubRepository] used for fetching repository data.
 */
@HiltViewModel
class RepositoryViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoryUiState())
    /** StateFlow emitting the current [RepositoryUiState]. */
    val uiState: StateFlow<RepositoryUiState> = _uiState.asStateFlow()

    /**
     * Loads the details and README content for the specified repository.
     * Updates the [uiState] with loading status, data, and potential errors.
     *
     * @param owner The owner of the repository.
     * @param repoName The name of the repository.
     */
    fun loadRepositoryDetails(owner: String, repoName: String) {
        viewModelScope.launch {
            // Combine flows for repository details and README to load them concurrently
            combine(
                repository.getRepository(owner, repoName).onStart { 
                    // Set loading state at the start of the combined flow
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null) 
                },
                repository.getReadme(owner, repoName)
            ) { repoResult, readmeResult ->
                // Package results into a Pair for the collector
                Pair(repoResult, readmeResult)
            }.catch { e ->
                // Catch exceptions during the flow combination (e.g., network issues)
                // TODO: Consider more specific error handling/messages based on exception type
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load repository data: ${e.message}")
            }.collect { (repoResult, readmeResult) ->
                // Process the results once both flows have emitted
                val repo = repoResult.getOrNull()
                val readme = readmeResult.getOrNull()
                val repoError = if (repoResult.isFailure) repoResult.exceptionOrNull()?.message else null
                val readmeError = if (readmeResult.isFailure) readmeResult.exceptionOrNull()?.message else null

                // Combine error messages, ignoring "No README found"
                var combinedError: String? = null
                if (repoError != null) {
                    combinedError = "Repository: $repoError"
                }
                if (readmeError != null && readmeError != "No README found") {
                    val readmeErrorMsg = "README: $readmeError"
                    combinedError = combinedError?.plus("\n$readmeErrorMsg") ?: readmeErrorMsg
                }
                // TODO: Consider moving error strings to resources for localization.

                // Update the final UI state
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    repository = repo,
                    // Assuming readme.content is already Base64 decoded by repository/model layer.
                    // If not, decode here: readme?.content?.let { Base64.decode... } ?: ""
                    readmeContent = readme?.content,
                    error = combinedError // Set final combined error message
                )
            }
        }
    }
} 