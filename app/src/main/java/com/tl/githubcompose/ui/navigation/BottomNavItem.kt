package com.tl.githubcompose.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents items available in the bottom navigation bar.
 *
 * @param route The navigation route associated with this item.
 * @param icon The icon to display for this item.
 * @param label The text label for this item.
 */
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    /** Represents the 'Popular Repositories' screen tab. */
    object Popular : BottomNavItem("popular", Icons.Default.Home, "Home")
    /** Represents the 'Search Repositories' screen tab. */
    object Search : BottomNavItem("search", Icons.Default.Search, "Search") // Placeholder for Search
    /** Represents the 'Profile'/'Login' screen tab. Acts as a wrapper. */
    object ProfileNav : BottomNavItem("profile_nav", Icons.Default.Person, "Me") // Wrapper for Profile/Login flow
}