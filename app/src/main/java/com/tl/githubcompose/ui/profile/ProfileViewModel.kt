package com.tl.githubcompose.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tl.githubcompose.data.GithubRepository
import com.tl.githubcompose.data.model.Repository
import com.tl.githubcompose.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing the UI state for the Profile screen.
 *
 * @param isLoading True if loading user profile or pinned repositories is in progress.
 * @param user The loaded [User] object for the authenticated user (null if not loaded or error).
 * @param pinnedRepositories The list of repositories fetched (intended to be pinned, see note in ViewModel).
 * @param error An optional error message if loading profile or pinned repos failed.
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val pinnedRepositories: List<Repository> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for the Profile screen.
 * Fetches the authenticated user's profile information and their pinned repositories.
 *
 * @param repository The [GithubRepository] used for fetching user and repository data.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    /** StateFlow emitting the current [ProfileUiState]. */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Loads the authenticated user's profile information.
     * On success, proceeds to load pinned repositories.
     * Updates the [uiState] with loading status, data, and errors.
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true) // Start fresh on load/reload
            repository.getCurrentUser()
                // No need for onStart here as we set isLoading=true above
                .catch { e ->
                    // Catch exceptions during the user profile flow itself
                    _uiState.value = ProfileUiState(isLoading = false, error = e.message ?: "Unknown error loading profile")
                    // Don't proceed to load pinned repos if user profile failed
                }
                .collect { result ->
                    result.onSuccess { user ->
                        // User loaded successfully, update state (isLoading will be handled by pinned repo load)
                        _uiState.value = ProfileUiState(isLoading = true, user = user) // Keep loading true while fetching pinned repos
                        // Load pinned repos now that we have the username
                        loadRecentRepos(user.login)
                    }.onFailure { e ->
                        // Handle failure from the API response for user profile
                        _uiState.value = ProfileUiState(isLoading = false, error = e.message ?: "Failed to load profile")
                        // Don't proceed to load pinned repos if user profile failed
                    }
                }
        }
    }

    /**
     * Loads the user's recent repositories (or recently pushed, see note).
     * Updates the [uiState], preserving existing user data and handling loading/error states.
     *
     * Note: Relies on `repository.getRecentlyPushedRepos` which might actually fetch recently pushed repos.
     * Verify the implementation in [GithubRepository] and [GithubApiService].
     *
     * @param username The login name of the user whose recent repos should be loaded.
     */
    private fun loadRecentRepos(username: String) {
        viewModelScope.launch {
            // Fetch recently pushed repos (method name corrected)
            repository.getRecentlyPushedRepos(username)
                .catch { e ->
                    // Catch exceptions during the repo flow
                    Log.e("ProfileViewModel", "Error loading recently pushed repos (catch)", e)
                    // Keep existing user data, update error and loading status
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error loading recently pushed repos")
                    // TODO: Consider combining errors if profile also had an error?
                }
                .collect { result ->
                    result.onSuccess { repos ->
                        // Repos loaded successfully, update state
                        _uiState.value = _uiState.value.copy(isLoading = false, pinnedRepositories = repos, error = null) // Clear error on success
                    }.onFailure { e ->
                        // Handle failure from the API response
                        Log.e("ProfileViewModel", "Error loading recently pushed repos (failure)", e)
                        // Keep existing user data, update error and loading status
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load recently pushed repos")
                        // TODO: Consider combining errors if profile also had an error?
                    }
                }
        }
    }
    // TODO: Consider moving error strings to resources for localization.
}