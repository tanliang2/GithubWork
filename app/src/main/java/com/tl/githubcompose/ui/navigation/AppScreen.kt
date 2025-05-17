package com.tl.githubcompose.ui.navigation

/**
 * Defines the possible navigation destinations within the application.
 * Each object represents a screen or feature accessible via Compose Navigation.
 *
 * @param route The navigation route string associated with the screen.
 *              Routes with parameters use curly braces `{}` for arguments.
 */
sealed class AppScreen(val route: String) {
    /** Initial loading or splash screen (if any). */
    object Loading : AppScreen("loading")
    /** Login screen for GitHub authentication. */
    object Login : AppScreen("login")
    /** User profile screen. */
    object Profile : AppScreen("profile")
    /** Screen displaying the user's repositories. */
    object Repositories : AppScreen("repositories")

    /**
     * Screen displaying details of a specific repository.
     * Requires `owner` and `repoName` arguments in the route.
     */
    object Repository : AppScreen("repository/{owner}/{repoName}") {
        /** Creates the navigation route for the Repository screen with the given parameters. */
        fun createRoute(owner: String, repoName: String) = "repository/$owner/$repoName"
    }

    /**
     * Screen displaying the list of issues for a specific repository.
     * Requires `owner` and `repoName` arguments in the route.
     */
    object Issues : AppScreen("repository/{owner}/{repoName}/issues") {
        /** Creates the navigation route for the Issues screen with the given parameters. */
        fun createRoute(owner: String, repoName: String) = "repository/$owner/$repoName/issues"
    }

    /**
     * Screen for creating a new issue in a specific repository.
     * Requires `owner` and `repoName` arguments in the route.
     */
    object RaiseIssue : AppScreen("repository/{owner}/{repoName}/issues/new") {
        /** Creates the navigation route for the RaiseIssue screen with the given parameters. */
        fun createRoute(owner: String, repoName: String) = "repository/$owner/$repoName/issues/new"
    }

    /**
     * Screen displaying the details of a specific issue.
     * Requires `owner`, `repoName`, and `issueNumber` arguments in the route.
     */
    object IssueDetail : AppScreen("repository/{owner}/{repoName}/issues/{issueNumber}") {
        /** Creates the navigation route for the IssueDetail screen with the given parameters. */
        fun createRoute(owner: String, repoName: String, issueNumber: Int) =
            "repository/$owner/$repoName/issues/$issueNumber"
    }
} 