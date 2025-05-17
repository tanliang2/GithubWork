package com.tl.githubcompose.data

import android.util.Log
import com.tl.githubcompose.BuildConfig
import com.tl.githubcompose.data.model.Problem
import com.tl.githubcompose.data.model.ProblemRequestBody
import com.tl.githubcompose.data.model.Readme
import com.tl.githubcompose.data.model.Repository
import com.tl.githubcompose.data.model.User
import com.tl.githubcompose.data.network.GithubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [GithubRepository] for interacting with the GitHub API.
 */
@Singleton // Mark as Singleton if appropriate for Hilt
class GithubRepositoryImpl @Inject constructor(
    private val apiService: GithubApiService,
    private val dataStore: DataStore
) : GithubRepository { 

    private val clientId = BuildConfig.GITHUB_CLIENT_ID
    private val clientSecret = BuildConfig.GITHUB_SECRET
    private val redirectUri = "tl://callback"

    override fun exchangeCodeForToken(code: String): Flow<Result<String>> = flow {
        try {
            val response = apiService.exchangeCodeForToken(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                redirectUri = redirectUri
            )
            if (response.isSuccessful && response.body()?.accessToken != null) {
                val token = response.body()!!.accessToken
                saveAuthToken(token) // Use the overridden method
                emit(Result.success(token))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("GithubRepositoryImpl", "Token exchange failed: ${response.code()} - $errorBody")
                emit(Result.failure(Exception("Token exchange failed: ${response.code()} - ${response.message()}")))
            }
        } catch (e: Exception) {
            Log.e("GithubRepositoryImpl", "Token exchange exception", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCurrentUser(): Flow<Result<User>> = flow {
        val token = getToken()
        if (token == null) {
            emit(Result.failure(Exception("Not authenticated")))
            return@flow
        }
        try {
            val response = apiService.getCurrentUser() // Assumes interceptor adds token
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("API Error getting user: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun searchRepos(query: String, page: Int, perPage: Int): Flow<Result<List<Repository>>> = flow {
        try {
            val response = apiService.searchRepositories(
                query = query,
                sort = "stars",
                order = "desc",
                page = page,
                perPage = perPage
            )
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!.items))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun searchPopularRepos(page: Int, perPage: Int): Flow<Result<List<Repository>>> = flow {
        try {
            val response =
                apiService.searchRepositories(query = "stars:>1", sort = "stars", order = "desc", page = page, perPage = perPage)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!.items))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e)) // Network error etc.
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getUserRepos(page: Int, perPage: Int): Flow<Result<List<Repository>>> = flow {
        val token = getToken()
        if (token == null) {
            emit(Result.failure(Exception("Not authenticated")))
            return@flow
        }
        try {
            val userResponse = apiService.getCurrentUser() 
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                emit(Result.failure(Exception("Failed to get current user profile before fetching repos")))
                return@flow
            }
            val username = userResponse.body()!!.login
            val response = apiService.getUserRepos(
                username = username,
                page = page,
                perPage = perPage
            )
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createIssue(
        owner: String,
        repoName: String,
        title: String,
        body: String?
    ): Flow<Result<Problem>> = flow {
        try {
            val requestBody = ProblemRequestBody(title, body)
            val response = apiService.createIssue(
                owner,
                repoName,
                requestBody
            )
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getRecentlyPushedRepos(username: String): Flow<Result<List<Repository>>> = flow {
        try {
            val response = apiService.getRecentlyPushedRepos(username)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getRepository(owner: String, repoName: String): Flow<Result<Repository>> = flow {
        try {
            val response = apiService.getRepository(owner, repoName)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Error fetching repository: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getReadme(owner: String, repoName: String): Flow<Result<Readme>> = flow {
        try {
            val response = apiService.getReadme(owner, repoName)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else if (response.code() == 404) {
                 emit(Result.failure(Exception("No README found"))) // Specific handling for 404
            } else {
                emit(Result.failure(Exception("Error fetching README: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // Inject DataStoreHelper instead of accessing statically
    override fun saveAuthToken(token: String) { dataStore.saveToken(token) }
    override fun clearAuthToken() { dataStore.clearToken() }
    override fun isAuthenticated(): Boolean = dataStore.getToken() != null
    override fun getToken(): String? = dataStore.getToken()
    
    // Keep these methods within Impl if they are implementation details
    // Or move to interface if they need to be part of the contract
    // For now, keeping them private/internal to Impl if possible, 
    // or removing if replaced by injected DataStoreHelper
    override suspend fun getIssues(owner: String, repoName: String, page: Int, perPage: Int): List<Problem> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getIssues(owner, repoName, page, perPage)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("Failed to fetch issues: ${response.code()} - $errorBody")
            }
        }
    }

    override suspend fun getIssueDetail(owner: String, repoName: String, issueNumber: Int): Problem {
        return withContext(Dispatchers.IO) {
            val response = apiService.getIssueDetail(owner, repoName, issueNumber)
            if (response.isSuccessful && response.body() != null) {
                 response.body()!!
            } else {
                 val errorBody = response.errorBody()?.string() ?: "Unknown error"
                 throw Exception("Failed to fetch issue detail: ${response.code()} - $errorBody")
            }
        }
    }
} 