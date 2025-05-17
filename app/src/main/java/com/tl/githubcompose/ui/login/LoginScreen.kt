package com.tl.githubcompose.ui.login // Ensure this matches your package structure

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tl.githubcompose.BuildConfig
import com.tl.githubcompose.R
import java.util.UUID

// GitHub OAuth Constants
private const val GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize"
private const val GITHUB_CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
private const val REDIRECT_URI = "tl://callback" // Must match AndroidManifest and GitHub App settings
private const val OAUTH_SCOPE = "repo,user" // Request repo and user info scopes


@Composable
fun LoginScreen(
    onLoginClick: (Context, String, String) -> Unit = ::launchCustomTab // Pass context, url, state
) {
    val context = LocalContext.current
    // Generate a unique state parameter for CSRF protection
    val state = remember { UUID.randomUUID().toString() }
    // TODO: Store the 'state' variable securely (e.g., ViewModel, SharedPreferences)
    //       to verify it upon receiving the OAuth callback.

    val authUrl = "$GITHUB_AUTH_URL?client_id=$GITHUB_CLIENT_ID"
        .toUri()
        .buildUpon()
        .appendQueryParameter("scope", OAUTH_SCOPE)
        .appendQueryParameter("state", state)
        .appendQueryParameter("redirect_uri", REDIRECT_URI)
        .build()
        .toString()

    // Configure status bar appearance for this screen
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.White,
            darkIcons = true // Use dark icons on light status bar
        )
        // Optional: Hide navigation bar if desired for a more immersive login screen
        // systemUiController.isNavigationBarVisible = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Set background to white
        horizontalAlignment = Alignment.CenterHorizontally // Center children horizontally
    ) {
        // Top space for the Logo
        Box(
            modifier = Modifier
                .weight(1f) // Occupy remaining vertical space, pushing the button to the bottom
                .fillMaxWidth(),
            contentAlignment = Alignment.Center // Center the Logo within this Box
        ) {
            // Bottom button area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 32.dp), // Add padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = stringResource(id = R.string.login_logo_description),
                    modifier = Modifier.size(100.dp) // Set Logo size
                )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { onLoginClick(context, authUrl, state) }, // Pass state
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.login_button_text),
                        fontSize = 16.sp
                    )
                }
            }

        }


    }
}

/**
 * Launches the GitHub OAuth URL in a Chrome Custom Tab if available,
 * otherwise falls back to opening in a standard browser.
 *
 * @param context The Android context.
 * @param url The authorization URL to launch.
 * @param state The generated state parameter (currently unused in this function, but passed for completeness).
 */
fun launchCustomTab(context: Context, url: String, state: String) {
    val uri = url.toUri()
    // Find a package that supports Custom Tabs
    val packageName = CustomTabsClient.getPackageName(context, null)

    if (packageName == null) {
        // Fallback: No CCT provider found. Open in standard browser.
        Log.w("LoginScreen", "No Custom Tabs provider found. Opening in standard browser.")
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("LoginScreen", "Could not launch any browser for URL: $url", e)
            // Optionally show a Toast or Snackbar to the user
        }
        return
    }

    // CCT provider found, launch the Custom Tab
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        // .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary)) // Example: Customize color
        .build()
    customTabsIntent.intent.setPackage(packageName)
    try {
        customTabsIntent.launchUrl(context, uri)
    } catch (e: Exception) {
        Log.e("LoginScreen", "Error launching Custom Tab for URL: $url", e)
        // Fallback to standard browser if CCT launch fails unexpectedly
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e2: Exception) {
            Log.e("LoginScreen", "Could not launch standard browser either for URL: $url", e2)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF) // Preview with white background
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginClick = { _, _, _ -> /* Do nothing in preview click */ })
}