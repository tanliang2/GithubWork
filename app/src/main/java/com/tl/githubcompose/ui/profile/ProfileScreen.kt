package com.tl.githubcompose.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.tl.githubcompose.R
import com.tl.githubcompose.data.model.Repository
import com.tl.githubcompose.data.model.User
import com.tl.githubcompose.ui.components.ErrorRetry
import com.tl.githubcompose.ui.login.AuthViewModel
import com.tl.githubcompose.ui.navigation.AppScreen

/**
 * Composable function for the user Profile screen.
 * Displays user information, allows logout, and shows pinned repositories.
 *
 * @param profileViewModel The [ProfileViewModel] providing user and repository data.
 * @param authViewModel The [AuthViewModel] used for logout functionality.
 * @param navController The [NavController] for navigating to other screens (e.g., All Repositories, Repository Detail).
 */
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by profileViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                ErrorRetry(
                    message = stringResource(id = R.string.error_profile_load_failed, uiState.error ?: ""),
                    onRetry = { profileViewModel.loadUserProfile() }
                )
            }
            uiState.user != null -> {
                UserProfileContent(
                    uiState = uiState, 
                    navController = navController,
                    onLogout = { authViewModel.logout() }
                )
            }
        }
    }
}

/**
 * Composable function displaying the main content of the user profile,
 * including header, bio, details, and repository section.
 *
 * @param uiState The current [ProfileUiState] containing user and repository data.
 * @param navController The [NavController] for navigation actions.
 * @param onLogout Callback invoked when the logout button is clicked.
 */
@Composable
fun UserProfileContent(
    uiState: ProfileUiState,
    navController: NavController,
    onLogout: () -> Unit
) {
    val user = uiState.user!!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header section (Avatar, Name, Username, Logout Button)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = rememberAsyncImagePainter(user.avatarUrl),
                contentDescription = stringResource(id = R.string.profile_avatar_description, user.login),
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name ?: user.login, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = user.login, fontSize = 16.sp, color = Color.Gray)
            }
            TextButton(onClick = onLogout) {
                Text(stringResource(id = R.string.profile_logout_button))
             }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bio or Status
        user.bio?.let { InfoRow(icon = Icons.Default.TagFaces, text = it) }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Other details like Company, Location, Blog, Email
        user.company?.let { InfoRow(icon = Icons.Default.Business, text = it) }
        user.location?.let { InfoRow(icon = Icons.Default.LocationOn, text = it) }
        user.blog?.let { if (it.isNotEmpty()) InfoRow(icon = Icons.Default.Link, text = it) }
        user.email?.let { InfoRow(icon = Icons.Default.Email, text = it) }

        Spacer(modifier = Modifier.height(8.dp))

        // Followers / Following
        val followers = user.followers ?: 0
        val following = user.following ?: 0
        InfoRow(icon = Icons.Default.People, text = stringResource(id = R.string.profile_followers_following, followers, following))

        Spacer(modifier = Modifier.height(16.dp))

        // Repository Section
        RepositorySection(
            uiState = uiState,
            navController = navController,
            onViewAll = { navController.navigate(AppScreen.Repositories.route) }
        )
    }
}

/**
 * Composable function displaying the Pinned Repositories section on the profile.
 *
 * @param uiState The current [ProfileUiState].
 * @param navController The [NavController] for navigation.
 * @param onViewAll Callback invoked when the "View All" button is clicked.
 */
@Composable
fun RepositorySection(
    uiState: ProfileUiState,
    navController: NavController,
    onViewAll: () -> Unit
) {
    Column {
        // Repository Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.profile_repositories_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onViewAll) {
                Text(stringResource(id = R.string.profile_view_all_button))
            }
        }

        // Pinned Repositories
        if (uiState.pinnedRepositories.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.profile_recent_title),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.pinnedRepositories.forEach { repo ->
                    RepositoryCard(repository = repo, onClick = {
                        navController.navigate(AppScreen.Repository.createRoute(repo.owner.login, repo.name))
                    })
                }
            }
        } else if (!uiState.isLoading) {
            Text(stringResource(id = R.string.profile_no_recent_repos), modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)
        }
    }
}

/**
 * Composable function displaying a single repository as a Card.
 * Used here for pinned repositories.
 *
 * Note: Defined locally within ProfileScreen.kt. Consider moving to ui/components
 * and potentially merging with [com.tl.githubcompose.ui.components.RepoItem]
 * if the required style and information are similar enough.
 *
 * @param repository The [Repository] data to display.
 * @param onClick Callback invoked when the card is clicked.
 */
@Composable
fun RepositoryCard(
    repository: Repository,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = repository.name,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = stringResource(id = R.string.repo_item_stars_description), tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${repository.stargazersCount}", color = Color.Gray)

                repository.language?.let {
                    Spacer(modifier = Modifier.width(16.dp))
                    // Simple dot divider
                    Text(" • ", color = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = it, color = Color.Gray)
                }
            }

            repository.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * A helper composable to display a row of information with an icon and text.
 *
 * @param icon The leading [ImageVector] icon.
 * @param text The text content to display.
 */
@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = text, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.body1)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val dummyUser = User(
        login = "octocat",
        id = 1,
        avatarUrl = "",
        htmlUrl = "https://github.com/octocat",
        name = "The Octocat",
        company = "GitHub",
        blog = "github.blog",
        location = "San Francisco",
        email = "octocat@github.com",
        bio = "This is a bio",
        followers = 100,
        following = 10
    )
    val dummyRepository = Repository(
        id = 1,
        name = "Spoon-Knife",
        fullName = "octocat/Spoon-Knife",
        owner = dummyUser,
        description = "This repo is for demonstration purposes only.",
        stargazersCount = 1000,
        forksCount = 100,
        language = "HTML",
        htmlUrl = "https://github.com/octocat/Spoon-Knife"
    )
    val dummyUiState = ProfileUiState(user = dummyUser, pinnedRepositories = List(3) { dummyRepository })
    val navController = rememberNavController()

    MaterialTheme {
        UserProfileContent(uiState = dummyUiState, navController = navController, onLogout = {}) 
    }
}

@Preview(showBackground = true)
@Composable
fun RepositoryCardPreview() {
    val dummyUser = User(
        login = "octocat",
        id = 1,
        avatarUrl = "",
        htmlUrl = "https://github.com/octocat",
        name = "The Octocat",
        company = "GitHub",
        blog = "github.blog",
        location = "San Francisco",
        email = "octocat@github.com",
        bio = "This is a bio",
        followers = 100,
        following = 10
    )
    val dummyRepository = Repository(
        id = 1,
        name = "Long-Repository-Name-Here-To-Test-Overflow",
        fullName = "octocat/Spoon-Knife",
        owner = dummyUser,
        description = "This is a very long description to test how text wrapping and ellipsis work in the card preview.",
        stargazersCount = 12345,
        forksCount = 500,
        language = "Kotlin",
        htmlUrl = "https://github.com/octocat/Spoon-Knife"
    )
    MaterialTheme {
        RepositoryCard(repository = dummyRepository, onClick = {})
    }
}