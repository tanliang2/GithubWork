package com.tl.githubcompose.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tl.githubcompose.ui.issue.IssueDetailScreen
import com.tl.githubcompose.ui.issue.RaiseIssueScreen
import com.tl.githubcompose.ui.issues.IssuesScreen
import com.tl.githubcompose.ui.login.AuthState
import com.tl.githubcompose.ui.login.AuthViewModel
import com.tl.githubcompose.ui.login.LoginScreen
import com.tl.githubcompose.ui.popular.PopularReposScreen
import com.tl.githubcompose.ui.profile.ProfileScreen
import com.tl.githubcompose.ui.repositories.RepositoriesScreen
import com.tl.githubcompose.ui.repository.RepositoryScreen
import com.tl.githubcompose.ui.search.SearchScreen

/**
 * Sets up the main navigation structure of the application using Jetpack Compose Navigation.
 *
 * Includes a Scaffold with a BottomNavigation bar and a NavHost to manage transitions
 * between different screens ([Composable] functions).
 *
 * @param authViewModel The [AuthViewModel] instance, obtained via Hilt, used to check authentication state.
 */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val items = listOf(
        BottomNavItem.Popular,
        BottomNavItem.Search,
        BottomNavItem.ProfileNav
    )

    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = Color.White,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        selectedContentColor = MaterialTheme.colors.primary,
                        unselectedContentColor = Color.Gray,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Popular.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Popular.route) {
                    PopularReposScreen(navController = navController)
                }
                composable(BottomNavItem.Search.route) {
                    SearchScreen(navController = navController)
                }
                composable(BottomNavItem.ProfileNav.route) {
                    when (authState) {
                        is AuthState.Authenticated -> ProfileScreen(
                            navController = navController,
                            authViewModel = authViewModel
                        )
                        else -> LoginScreen()
                    }
                }
                composable(AppScreen.Login.route) {
                    LoginScreen()
                }
                composable(AppScreen.Repositories.route) {
                    RepositoriesScreen(navController = navController)
                }
                composable(
                    route = AppScreen.Repository.route,
                    arguments = listOf(
                        navArgument("owner") { type = NavType.StringType },
                        navArgument("repoName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val owner = backStackEntry.arguments?.getString("owner").orEmpty()
                    val repoName = backStackEntry.arguments?.getString("repoName").orEmpty()
                    RepositoryScreen(
                        owner = owner,
                        repoName = repoName,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToRaiseIssue = { ownerParam, repoNameParam ->
                            navController.navigate(AppScreen.RaiseIssue.createRoute(ownerParam, repoNameParam))
                        },
                        onNavigateToIssues = { ownerParam, repoNameParam ->
                            navController.navigate(AppScreen.Issues.createRoute(ownerParam, repoNameParam))
                        }
                    )
                }

                composable(
                    route = AppScreen.RaiseIssue.route,
                    arguments = listOf(
                        navArgument("owner") { type = NavType.StringType },
                        navArgument("repoName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val owner = backStackEntry.arguments?.getString("owner").orEmpty()
                    val repoName = backStackEntry.arguments?.getString("repoName").orEmpty()
                    RaiseIssueScreen(
                        owner = owner,
                        repoName = repoName,
                        onNavigateBack = { navController.navigateUp() },
                        onIssueCreated = { issueNumber ->
                            navController.navigate(AppScreen.IssueDetail.createRoute(owner, repoName, issueNumber)) {
                                popUpTo(AppScreen.RaiseIssue.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = AppScreen.IssueDetail.route,
                    arguments = listOf(
                        navArgument("owner") { type = NavType.StringType },
                        navArgument("repoName") { type = NavType.StringType },
                        navArgument("issueNumber") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val owner = backStackEntry.arguments?.getString("owner").orEmpty()
                    val repoName = backStackEntry.arguments?.getString("repoName").orEmpty()
                    val issueNumber = backStackEntry.arguments?.getInt("issueNumber") ?: 0
                    IssueDetailScreen(
                        owner = owner,
                        repoName = repoName,
                        issueNumber = issueNumber,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                composable(
                    route = AppScreen.Issues.route,
                    arguments = listOf(
                        navArgument("owner") { type = NavType.StringType },
                        navArgument("repoName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val owner = backStackEntry.arguments?.getString("owner").orEmpty()
                    val repoName = backStackEntry.arguments?.getString("repoName").orEmpty()
                    IssuesScreen(
                        owner = owner,
                        repoName = repoName,
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToDetail = { ownerParam, repoNameParam, issueNumber ->
                            navController.navigate(AppScreen.IssueDetail.createRoute(ownerParam, repoNameParam, issueNumber))
                        }
                    )
                }
            }
        }
    }
} 