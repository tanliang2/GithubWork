package com.tl.githubcompose.data

import com.google.common.truth.Truth.assertThat
import com.tl.githubcompose.data.model.Problem
import com.tl.githubcompose.data.model.ProblemRequestBody
import com.tl.githubcompose.data.model.Repository
import com.tl.githubcompose.data.model.SearchResult
import com.tl.githubcompose.data.model.User
import com.tl.githubcompose.data.network.GithubApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * Unit tests for [GithubRepository].
 * Tests the repository layer's interaction with the GitHub API service and data store.
 *
 * Key test areas:
 * - User authentication and profile retrieval
 * - Repository search functionality
 * - Issue creation and management
 * - Error handling and edge cases
 */
@ExperimentalCoroutinesApi
class GithubRepositoryTest {

    companion object {
        // Test data constants
        private const val FAKE_TOKEN = "fake-token"
        private const val TEST_OWNER = "testowner"
        private const val TEST_REPO = "testrepo"
        private const val TEST_ISSUE_TITLE = "Test Issue"
        private const val TEST_ISSUE_BODY = "Test Body"
        private const val ERROR_NOT_AUTHENTICATED = "Not authenticated"
        
        // API parameters
        private const val SORT_STARS = "stars"
        private const val ORDER_DESC = "desc"
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PER_PAGE = 20
        private const val POPULAR_REPOS_QUERY = "stars:>1"
        
        // Mock user data
        private val TEST_USER = User(
            login = "testuser",
            id = 1,
            avatarUrl = "http://example.com/avatar.jpg"
        )
        
        // Mock repository data
        private fun createMockRepo(id: Long, name: String, stars: Int) = Repository(
            id = id,
            name = name,
            fullName = "user$id/$name",
            owner = User(login = "user$id", id = id, avatarUrl = ""),
            description = "desc$id",
            stargazersCount = stars,
            forksCount = stars / 2,
            language = if (id.toInt() % 2 == 0) "Java" else "Kotlin",
            htmlUrl = "https://github.com/user$id/$name"
        )
        
        // Mock issue data
        private val TEST_Problem = Problem(
            id = 1,
            number = 1,
            title = TEST_ISSUE_TITLE,
            body = TEST_ISSUE_BODY,
            state = "open",
            createdAt = "2024-03-20T10:00:00Z",
            updatedAt = "2024-03-20T10:00:00Z",
            user = TEST_USER
        )
    }

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockApiService: GithubApiService

    @Mock
    private lateinit var mockDataStore: DataStore

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: GithubRepository

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        repository = GithubRepositoryImpl(mockApiService, mockDataStore)
    }

    // User Authentication Tests
    
    @Test
    fun `getCurrentUser returns failure when not authenticated`() = runTest(testDispatcher) {
        // Given
        whenever(mockDataStore.getToken()).thenReturn(null)

        // When
        val result = repository.getCurrentUser().first()

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(ERROR_NOT_AUTHENTICATED)
        verify(mockApiService, never()).getCurrentUser()
    }

    @Test
    fun `getCurrentUser returns user on successful API call`() = runTest(testDispatcher) {
        // Given
        val successResponse: Response<User> = Response.success(TEST_USER)
        whenever(mockDataStore.getToken()).thenReturn(FAKE_TOKEN)
        whenever(mockApiService.getCurrentUser()).thenReturn(successResponse)

        // When
        val result = repository.getCurrentUser().first()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(TEST_USER)
        verify(mockApiService).getCurrentUser()
    }

    @Test
    fun `getCurrentUser returns failure on API error`() = runTest(testDispatcher) {
        // Given
        val errorResponse: Response<User> = Response.error(404, "Not Found".toResponseBody(null))
        whenever(mockDataStore.getToken()).thenReturn(FAKE_TOKEN)
        whenever(mockApiService.getCurrentUser()).thenReturn(errorResponse)

        // When
        val result = repository.getCurrentUser().first()

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("API Error getting user: 404")
        verify(mockApiService).getCurrentUser()
    }

    @Test
    fun `getCurrentUser returns failure on API exception`() = runTest(testDispatcher) {
        // Given
        val exception = RuntimeException("Network error")
        whenever(mockDataStore.getToken()).thenReturn(FAKE_TOKEN)
        whenever(mockApiService.getCurrentUser()).doSuspendableAnswer { throw exception }

        // When
        val result = repository.getCurrentUser().first()

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
        verify(mockApiService).getCurrentUser()
    }

    // Repository Search Tests

    @Test
    fun `searchRepos returns success with repositories list`() = runTest(testDispatcher) {
        // Given
        val mockRepos = listOf(
            createMockRepo(1, "repo1", 100),
            createMockRepo(2, "repo2", 200)
        )
        val searchResult = SearchResult(
            totalCount = mockRepos.size,
            incompleteResults = false,
            items = mockRepos
        )
        val successResponse: Response<SearchResult> = Response.success(searchResult)
        whenever(mockApiService.searchRepositories(
            query = "test",
            sort = SORT_STARS,
            order = ORDER_DESC,
            page = DEFAULT_PAGE,
            perPage = DEFAULT_PER_PAGE
        )).thenReturn(successResponse)

        // When
        val result = repository.searchRepos("test").first()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(mockRepos)
    }

    @Test
    fun `searchPopularRepos returns success with popular repositories`() = runTest(testDispatcher) {
        // Given
        val mockRepos = listOf(
            createMockRepo(1, "popular1", 1000),
            createMockRepo(2, "popular2", 2000)
        )
        val searchResult = SearchResult(
            totalCount = mockRepos.size,
            incompleteResults = false,
            items = mockRepos
        )
        val successResponse: Response<SearchResult> = Response.success(searchResult)
        whenever(mockApiService.searchRepositories(
            query = POPULAR_REPOS_QUERY,
            sort = SORT_STARS,
            order = ORDER_DESC,
            page = DEFAULT_PAGE,
            perPage = DEFAULT_PER_PAGE
        )).thenReturn(successResponse)

        // When
        val result = repository.searchPopularRepos().first()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(mockRepos)
    }

    // Issue Management Tests

    @Test
    fun `createIssue returns success with created issue`() = runTest(testDispatcher) {
        // Given
        val successResponse: Response<Problem> = Response.success(TEST_Problem)
        whenever(mockApiService.createIssue(
            owner = TEST_OWNER,
            repo = TEST_REPO,
            problemRequestBody = ProblemRequestBody(title = TEST_ISSUE_TITLE, body = TEST_ISSUE_BODY)
        )).thenReturn(successResponse)

        // When
        val result = repository.createIssue(
            owner = TEST_OWNER,
            repoName = TEST_REPO,
            title = TEST_ISSUE_TITLE,
            body = TEST_ISSUE_BODY
        ).first()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(TEST_Problem)
    }

    @Test
    fun `createIssue returns failure on API error`() = runTest(testDispatcher) {
        // Given
        val errorResponse: Response<Problem> = Response.error(403, "Forbidden".toResponseBody(null))
        whenever(mockApiService.createIssue(
            owner = TEST_OWNER,
            repo = TEST_REPO,
            problemRequestBody = ProblemRequestBody(title = TEST_ISSUE_TITLE, body = TEST_ISSUE_BODY)
        )).thenReturn(errorResponse)

        // When
        val result = repository.createIssue(
            owner = TEST_OWNER,
            repoName = TEST_REPO,
            title = TEST_ISSUE_TITLE,
            body = TEST_ISSUE_BODY
        ).first()

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("API Error: 403")
    }
}
