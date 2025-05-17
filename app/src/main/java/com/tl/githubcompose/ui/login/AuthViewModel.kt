package com.tl.githubcompose.ui.login

import android.util.Log
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
 * Represents the different states of user authentication.
 */
sealed class AuthState {
    /** Initial state before the authentication status has been determined. */
    object Unknown : AuthState()
    /** State indicating the user is confirmed to be not logged in. */
    object Unauthenticated : AuthState()
    /** State indicating the user is successfully logged in. */
    object Authenticated : AuthState()
    /** State indicating an authentication operation (like token exchange) is in progress. */
    object Loading : AuthState()
    /** State indicating an error occurred during an authentication process. */
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel responsible for managing the user authentication state and related operations,
 * such as login (token exchange) and logout.
 *
 * @param repository The [GithubRepository] used for authentication-related data operations.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: GithubRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    /** StateFlow emitting the current [AuthState] of the user. */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkInitialAuthState()
    }

    /**
     * Checks the persistent storage via [GithubRepository] on initialization
     * to determine the initial authentication state.
     */
    private fun checkInitialAuthState() {
        // Check DataStore on startup
        viewModelScope.launch {
            if (repository.isAuthenticated()) {
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    /**
     * Exchanges the received authorization code for an access token via the repository.
     * Updates the [authState] to [Loading], then [Authenticated] on success
     * or [Error] on failure.
     * Should be called after the OAuth callback provides the code (e.g., in MainActivity).
     *
     * @param code The authorization code received from the GitHub OAuth callback.
     */
    fun exchangeCodeForToken(code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading // Indicate loading state
            repository.exchangeCodeForToken(code)
                .collect { result ->
                    result.onSuccess { token ->
                        _authState.value = AuthState.Authenticated
                        Log.i("AuthViewModel", "Token received and stored successfully.")
                    }.onFailure { e ->
                        _authState.value = AuthState.Error("Login failed: ${e.message}")
                        Log.e("AuthViewModel", "Token exchange failed", e)
                    }
                }
        }
    }

    /**
     * Logs the user out by clearing the stored authentication token
     * and updating the [authState] to [Unauthenticated].
     */
    fun logout() {
        viewModelScope.launch {
            repository.clearAuthToken()
            _authState.value = AuthState.Unauthenticated
        }
    }
}