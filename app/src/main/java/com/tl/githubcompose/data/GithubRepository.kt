package com.tl.githubcompose.data

import com.tl.githubcompose.data.model.Problem
import com.tl.githubcompose.data.model.Readme
import com.tl.githubcompose.data.model.Repository
import com.tl.githubcompose.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Interface for interacting with the GitHub API.
 * Defines the contract for data fetching and authentication token management.
 */
interface GithubRepository {

    /**
     * Exchanges an authorization code for an access token.
     */
    fun exchangeCodeForToken(code: String): Flow<Result<String>>

    /**
     * Fetches the currently authenticated user's profile information.
     */
    suspend fun getCurrentUser(): Flow<Result<User>>

    /**
     * Searches for repositories on GitHub based on a query.
     */
    suspend fun searchRepos(query: String, page: Int = 1, perPage: Int = 20): Flow<Result<List<Repository>>>

    /**
     * Searches for popular repositories on GitHub.
     */
    suspend fun searchPopularRepos(page: Int = 1, perPage: Int = 20): Flow<Result<List<Repository>>>

    /**
     * Fetches the repositories belonging to the currently authenticated user.
     */
    suspend fun getUserRepos(page: Int = 1, perPage: Int = 20): Flow<Result<List<Repository>>>

    /**
     * Creates a new issue in a specified repository.
     */
    suspend fun createIssue(
        owner: String,
        repoName: String,
        title: String,
        body: String?
    ): Flow<Result<Problem>>

    /**
     * Fetches the 6 most recently pushed repositories for a specified user.
     */
    suspend fun getRecentlyPushedRepos(username: String): Flow<Result<List<Repository>>>

    /**
     * Fetches details for a single repository.
     */
    fun getRepository(owner: String, repoName: String): Flow<Result<Repository>>

    /**
     * Fetches the README content for a specific repository.
     */
    suspend fun getReadme(owner: String, repoName: String): Flow<Result<Readme>>

    /** Saves the authentication token. */
    fun saveAuthToken(token: String)

    /** Clears the authentication token. */
    fun clearAuthToken()

    /** Checks if an authentication token exists. */
    fun isAuthenticated(): Boolean

    /** Retrieves the authentication token. */
    fun getToken(): String?

    /**
     * Fetches a list of issues for a specific repository.
     */
    suspend fun getIssues(owner: String, repoName: String, page: Int = 1, perPage: Int = 20): List<Problem>

    /**
     * Fetches the details for a single issue.
     */
    suspend fun getIssueDetail(owner: String, repoName: String, issueNumber: Int): Problem
}