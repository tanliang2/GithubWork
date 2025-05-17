package com.tl.githubcompose

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tl.githubcompose.ui.login.AuthViewModel
import com.tl.githubcompose.ui.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The main activity of the application, handling the entry point and OAuth callback.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()

        // Configure system bars for light mode (dark icons)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            // Remember the system UI controller
            val systemUiController = rememberSystemUiController()

            // Set the status bar color
            SideEffect {
                systemUiController.setStatusBarColor(
                    color = Color.White,
                    darkIcons = true
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Handles the incoming intent, specifically looking for the OAuth callback URI.
     * Extracts the authorization code or error details from the URI and passes
     * them to the [AuthViewModel].
     *
     * @param intent The intent to handle, potentially containing the OAuth callback URI.
     */
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        val scheme = uri?.scheme
        val host = uri?.host
        lifecycleScope.launch {
            // Check if this Intent is for our callback scheme ("tl") and host ("callback")
            if (scheme == "tl" && host == "callback") {
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    // Trigger token exchange in ViewModel
                    authViewModel.exchangeCodeForToken(code)
                } else {
                    // Handle error: Code is missing
                    val error = uri.getQueryParameter("error")
                    val errorDesc = uri.getQueryParameter("error_description")
                    Log.e("OAuthCallback", "OAuth Error: $error - $errorDesc")
                }
            }
        }
    }
} 