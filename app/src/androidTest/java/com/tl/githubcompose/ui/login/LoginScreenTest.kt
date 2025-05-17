package com.tl.githubcompose.ui.login

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.UriMatchers.hasHost
import androidx.test.espresso.intent.matcher.UriMatchers.hasPath
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tl.githubcompose.MainActivity
import com.tl.githubcompose.R
import com.tl.githubcompose.di.RepositoryModule
import com.tl.githubcompose.ui.navigation.BottomNavItem
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val intentsRule = IntentsRule()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun loginScreen_displays_when_profileTabClicked_andNotLoggedIn() {
        // Arrange: MainActivity is launched, start destination is Popular.

        // Act: Find and click the 'Profile' bottom navigation item.
        val profileNavLabel = BottomNavItem.ProfileNav.label // Use label from definition
        composeTestRule.onNodeWithText(profileNavLabel).performClick()

        // Assert: LoginScreen elements should now be displayed.
        // We might need waitUntil if there's a slight delay.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText(composeTestRule.activity.getString(R.string.login_button_text))
                .fetchSemanticsNodes().isNotEmpty()
        }

        val logoDescription = composeTestRule.activity.getString(R.string.login_logo_description)
        val buttonText = composeTestRule.activity.getString(R.string.login_button_text)

        composeTestRule.onNodeWithContentDescription(logoDescription).assertIsDisplayed()
        composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
    }

    @Test
    fun loginButton_launchesCorrectIntent() {
        // Arrange: Navigate to the Login Screen
        val profileNavLabel = BottomNavItem.ProfileNav.label
        composeTestRule.onNodeWithText(profileNavLabel).performClick()

        // Ensure Login Button is present before clicking
        val buttonText = composeTestRule.activity.getString(R.string.login_button_text)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText(buttonText)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(buttonText).assertIsDisplayed() // Double check

        // Stub the intent to prevent it from actually launching the browser
        Intents.intending(hasAction(Intent.ACTION_VIEW))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        // Act: Click the login button
        composeTestRule.onNodeWithText(buttonText).performClick()

        // Assert: Verify that an intent to the GitHub auth URL was sent
        Intents.intended(allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(hasHost("github.com")),
            hasData(hasPath("/login/oauth/authorize"))
        ))
    }
} 