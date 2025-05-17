package com.tl.githubcompose.ui.repository

import com.tl.githubcompose.data.model.Repository

/**
 * Data class representing the UI state for the Repository Detail screen.
 *
 * @param isLoading True if loading repository details or README content is in progress.
 * @param repository The loaded [Repository] object containing repository details (null if not loaded or error).
 * @param readmeContent The decoded content of the repository's README file (null if not found, not loaded, or error).
 * @param error An optional error message if loading repository details or README failed.
 */
data class RepositoryUiState(
    val isLoading: Boolean = false,
    val repository: Repository? = null,
    val readmeContent: String? = null,
    val error: String? = null
) 