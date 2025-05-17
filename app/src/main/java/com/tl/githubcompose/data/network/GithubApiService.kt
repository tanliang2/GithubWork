package com.tl.githubcompose.data.network

import com.tl.githubcompose.data.model.AccessTokenResp
import com.tl.githubcompose.data.model.Problem
import com.tl.githubcompose.data.model.ProblemRequestBody
import com.tl.githubcompose.data.model.Readme
import com.tl.githubcompose.data.model.Repository
import com.tl.githubcompose.data.model.SearchResult
import com.tl.githubcompose.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface GithubApiService {

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<SearchResult> // Define SearchResponse data class


    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String,
        @Query("type") type: String = "owner", // Consider if this is always desired
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("sort") sort: String = "updated" // Default changed from API default (full_name)
    ): Response<List<Repository>> // Define Repo data class


    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body problemRequestBody: ProblemRequestBody // Define IssueRequestBody data class
    ): Response<Problem> // Define Issue data class


    @FormUrlEncoded // Important: Send data as form-urlencoded
    @Headers("Accept: application/json") // Request JSON response
    @POST("https://github.com/login/oauth/access_token") // Full URL
    suspend fun exchangeCodeForToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String // Send the same redirect_uri used in auth request
    ): Response<AccessTokenResp>

    @GET("user")
    suspend fun getCurrentUser(): Response<User> // Assuming User data class exists

    /**
     * Fetches the 6 most recently pushed repositories for a specified user.
     * Note: This does NOT fetch the user's "Pinned" repositories which require the GraphQL API.
     * Uses the standard list repositories endpoint with specific sorting and pagination.
     * See: https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#list-repositories-for-a-user
     *
     * @param username The handle for the GitHub user account.
     * @return A [Response] containing a list of up to 6 [Repository] objects sorted by push date.
     */
    @GET("users/{username}/repos") // Same endpoint as getUserRepos, different params
    suspend fun getRecentlyPushedRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "pushed",
        @Query("per_page") perPage: Int = 6
    ): Response<List<Repository>>

    /**
     * Lists repositories for the *authenticated* user. Requires authentication.
     * See: https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#list-repositories-for-the-authenticated-user
     *
     * @param page Page number of the results to fetch.
     * @param perPage The number of results per page (max 100).
     * @return A [Response] containing a list of [Repository] objects.
     */
    @GET("user/repos")
    suspend fun getUserRepositories(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<List<Repository>>

    /**
     * Gets a single repository's details.
     * See: https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#get-a-repository
     *
     * @param owner The account owner of the repository. The name is not case sensitive.
     * @param repo The name of the repository. The name is not case sensitive.
     * @return A [Response] containing the [Repository] object.
     */
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(@Path("owner") owner: String, @Path("repo") repo: String): Response<Repository>

    /**
     * Gets the README file for a repository.
     * See: https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28#get-a-repository-readme
     *
     * @param owner The account owner of the repository. The name is not case sensitive.
     * @param repo The name of the repository. The name is not case sensitive.
     * @return A [Response] containing the [Readme] object (content is Base64 encoded).
     */
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(@Path("owner") owner: String, @Path("repo") repo: String): Response<Readme>

    /**
     * Lists issues for a repository.
     * See: https://docs.github.com/en/rest/issues/issues?apiVersion=2022-11-28#list-repository-issues
     *
     * @param owner The account owner of the repository. The name is not case sensitive.
     * @param repo The name of the repository. The name is not case sensitive.
     * @param page Page number of the results to fetch.
     * @param perPage The number of results per page (max 100).
     * @return A [Response] containing a list of [Problem] objects.
     */
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<List<Problem>>

    /**
     * Gets a single issue from a repository.
     * See: https://docs.github.com/en/rest/issues/issues?apiVersion=2022-11-28#get-an-issue
     *
     * @param owner The account owner of the repository. The name is not case sensitive.
     * @param repo The name of the repository. The name is not case sensitive.
     * @param issueNumber The number that identifies the issue.
     * @return A [Response] containing the [Problem] object.
     */
    @GET("repos/{owner}/{repo}/issues/{issue_number}")
    suspend fun getIssueDetail(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int
    ): Response<Problem>
}