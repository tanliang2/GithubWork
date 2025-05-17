package com.tl.githubcompose.ui.popular

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tl.githubcompose.MainActivity
import com.tl.githubcompose.data.GithubRepository
import com.tl.githubcompose.data.model.Problem
import com.tl.githubcompose.data.model.Readme
import com.tl.githubcompose.data.model.Repository
import com.tl.githubcompose.data.model.User
import com.tl.githubcompose.di.RepositoryModule
import com.tl.githubcompose.ui.components.ErrorRetryTags
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Singleton

// Shared test configuration and utility classes
object TestConfig {
    // Test timeouts and delays
    const val LOADING_TIMEOUT = 5000L
    const val SCROLL_DELAY = 300L
    const val NETWORK_DELAY = 1000L
    const val LOAD_MORE_DELAY = 2000L
    
    // Pagination
    const val ITEMS_PER_PAGE = 20
    const val SCROLL_REPEAT_COUNT = 3

    // Test state
    var shouldFail = false

    // Test data generators
    fun createMockUser(index: Int) = User(
        id = index.toLong(),
        login = "user$index",
        avatarUrl = "https://github.com/user$index.png"
    )

    fun createMockRepos(): List<Repository> = List(ITEMS_PER_PAGE) { index ->
        Repository(
            id = index.toLong(),
            name = "Repo $index",
            fullName = "Owner/Repo$index",
            description = "Description $index",
            owner = createMockUser(index),
            stargazersCount = index * 100,
            forksCount = index * 50,
            language = "Kotlin",
            htmlUrl = "https://github.com/owner$index/repo$index"
        )
    }
}

/**
 * UI tests for the Popular Repositories screen.
 * Tests various states and interactions of the screen including:
 * - Initial loading state
 * - Successful data loading
 * - Error handling
 * - Load more functionality
 */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class PopularReposScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakeRepository: GithubRepository

    @Before
    fun setUp() {
        TestConfig.shouldFail = false
        hiltRule.inject()
    }

    @Test
    fun loadingIndicator_isVisible_whenScreenLaunches() {
        // Wait for loading indicator to appear
        waitForNodeByTag(PopularScreenTags.LOADING_INDICATOR)
        
        // Verify loading state
        composeTestRule.onNodeWithTag(PopularScreenTags.LOADING_INDICATOR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PopularScreenTags.REPO_LIST).assertDoesNotExist()
    }

    @Test
    fun repoList_isDisplayed_whenDataLoadedSuccessfully() {
        // Wait for data to load
        waitForNodeByTag(PopularScreenTags.REPO_LIST)

        // Verify successful loading state
        composeTestRule.onNodeWithTag(PopularScreenTags.LOADING_INDICATOR).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PopularScreenTags.REPO_LIST).assertIsDisplayed()
    }

    @Test
    fun errorMessage_isDisplayed_whenLoadingFails() {
        TestConfig.shouldFail = true
        
        // Wait for error container to appear
        waitForNodeByTag(ErrorRetryTags.CONTAINER)

        // Verify error state
        composeTestRule.onNodeWithTag(ErrorRetryTags.MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ErrorRetryTags.BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PopularScreenTags.LOADING_INDICATOR).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PopularScreenTags.REPO_LIST).assertDoesNotExist()
    }

    @Test
    fun loadMoreIndicator_isDisplayed_whenLoadingMoreData() {
        // Wait for initial data to load
        waitForNodeByTag(PopularScreenTags.REPO_LIST)
        waitForNodeByText("Repo 0")

        // Scroll to bottom
        scrollToBottom()

        // Verify load more indicator
        waitForNodeByTag(PopularScreenTags.LOAD_MORE_INDICATOR)
        composeTestRule.onNodeWithTag(PopularScreenTags.LOAD_MORE_INDICATOR).assertIsDisplayed()
    }

    private fun waitForNodeByTag(tag: String, timeout: Long = TestConfig.LOADING_TIMEOUT) {
        composeTestRule.waitUntil(timeoutMillis = timeout) {
            composeTestRule
                .onAllNodesWithTag(tag)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForNodeByText(text: String, timeout: Long = TestConfig.LOADING_TIMEOUT) {
        composeTestRule.waitUntil(timeoutMillis = timeout) {
            composeTestRule
                .onAllNodesWithText(text)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollToBottom() {
        val list = composeTestRule.onNodeWithTag(PopularScreenTags.REPO_LIST)
        repeat(TestConfig.SCROLL_REPEAT_COUNT) {
            list.performScrollToIndex(TestConfig.ITEMS_PER_PAGE - 1)
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.mainClock.advanceTimeBy(TestConfig.SCROLL_DELAY)
            composeTestRule.mainClock.autoAdvance = true
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideGithubRepository(): GithubRepository {
        return object : GithubRepository {
            override suspend fun searchPopularRepos(page: Int, perPage: Int): Flow<Result<List<Repository>>> = flow {
                delay(TestConfig.NETWORK_DELAY)
                if (TestConfig.shouldFail) {
                    emit(Result.failure(Exception("Failed to load repositories")))
                } else if (page == 1) {
                    emit(Result.success(TestConfig.createMockRepos()))
                } else {
                    delay(TestConfig.LOAD_MORE_DELAY)
                    emit(Result.failure(Exception("Failed to load repositories")))
                }
            }

            override fun exchangeCodeForToken(code: String): Flow<Result<String>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override suspend fun getCurrentUser(): Flow<Result<User>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override suspend fun searchRepos(
                query: String,
                page: Int,
                perPage: Int
            ): Flow<Result<List<Repository>>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override suspend fun getUserRepos(
                page: Int,
                perPage: Int
            ): Flow<Result<List<Repository>>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override suspend fun createIssue(
                owner: String,
                repoName: String,
                title: String,
                body: String?
            ): Flow<Result<Problem>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override suspend fun getRecentlyPushedRepos(username: String): Flow<Result<List<Repository>>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override fun getRepository(owner: String, repoName: String): Flow<Result<Repository>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override suspend fun getReadme(owner: String, repoName: String): Flow<Result<Readme>> = flow {
                emit(Result.failure(Exception("Not implemented in test")))
            }

            override fun saveAuthToken(token: String) {
                // Not implemented in test
            }

            override fun clearAuthToken() {
                // Not implemented in test
            }

            override fun isAuthenticated(): Boolean = false

            override fun getToken(): String? = null

            override suspend fun getIssues(
                owner: String,
                repoName: String,
                page: Int,
                perPage: Int
            ): List<Problem> = emptyList()

            override suspend fun getIssueDetail(
                owner: String,
                repoName: String,
                issueNumber: Int
            ): Problem = throw NotImplementedError("Not implemented in test")
        }
    }
}