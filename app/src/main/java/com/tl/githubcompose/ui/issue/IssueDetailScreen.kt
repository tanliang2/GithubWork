package com.tl.githubcompose.ui.issue

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.tl.githubcompose.R
import com.tl.githubcompose.data.model.Problem
import com.tl.githubcompose.ui.components.ErrorRetry

/**
 * Composable function for the Issue Detail screen.
 * Displays the details of a specific issue, including title, status, author, and body.
 *
 * @param owner The repository owner.
 * @param repoName The repository name.
 * @param issueNumber The number of the issue to display.
 * @param viewModel The [IssueDetailViewModel] providing UI state and loading logic.
 * @param onNavigateBack Callback invoked when the back navigation action is triggered.
 */
@Composable
fun IssueDetailScreen(
    owner: String,
    repoName: String,
    issueNumber: Int,
    viewModel: IssueDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(owner, repoName, issueNumber) {
        // Load issue details when parameters change
        viewModel.loadIssueDetail(owner, repoName, issueNumber)
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.issue_detail_title, owner, repoName, issueNumber),
                        color = Color.Black,
                        style = MaterialTheme.typography.subtitle1
                    )
                },
                backgroundColor = Color.White,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    // TODO: Implement Share functionality
                    IconButton(onClick = { /* TODO: Implement Share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.action_share), // English description
                            tint = Color.Black
                        )
                    }
                    // TODO: Implement More Options menu
                    IconButton(onClick = { /* TODO: Implement More Options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more_options), // English description
                            tint = Color.Black
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    ErrorRetry(
                        message = uiState.error ?: stringResource(R.string.error_issue_detail_load_failed_default), 
                        onRetry = { viewModel.loadIssueDetail(owner, repoName, issueNumber) }
                    )
                }
                uiState.problem != null -> {
                    IssueDetailContent(problem = uiState.problem!!)
                }
            }
        }
    }
}

/**
 * Composable function displaying the main content of the issue details.
 *
 * @param problem The [Problem] object containing the details to display.
 */
@Composable
private fun IssueDetailContent(problem: Problem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = problem.title,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Status label (Open/Closed)
        val statusColor = if (problem.state.equals("open", ignoreCase = true)) Color(0xFF2EA44F) else Color.Gray
        val statusText = if (problem.state.equals("open", ignoreCase = true)) stringResource(R.string.issue_status_open) else stringResource(R.string.issue_status_closed)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(statusColor)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusText, // Dynamic status text
                color = Color.White,
                style = MaterialTheme.typography.body2
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Author Info
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(problem.user.avatarUrl),
                contentDescription = stringResource(R.string.profile_avatar_description, problem.user.login),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = problem.user.login,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    // TODO: Format this date/time properly (e.g., using java.time or a library)
                    text = stringResource(R.string.issue_opened_on, problem.createdAt /* Replace with formattedDate */),
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Body
        if (!problem.body.isNullOrBlank()) {
            Text(
                text = problem.body,
                style = MaterialTheme.typography.body1
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reaction Button
        OutlinedButton(
            onClick = { /* TODO: Implement reactions */ },
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEmotions,
                contentDescription = stringResource(R.string.issue_add_reaction),
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sub-issues Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.issue_sub_issues_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
            // TODO: Implement adding sub-issues functionality
            IconButton(onClick = { /* TODO: Add sub-issue */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.issue_add_sub_issue),
                    tint = MaterialTheme.colors.primary
                )
            }
        }
    }
} 