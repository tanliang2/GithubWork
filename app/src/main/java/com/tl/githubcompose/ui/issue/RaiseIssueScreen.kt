package com.tl.githubcompose.ui.issue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tl.githubcompose.R

/**
 * Composable function for the screen used to create a new issue.
 * Provides input fields for the issue title and body, and handles submission.
 *
 * Note: Considers moving title, body, and error message state management into the ViewModel
 * for better state hoisting and adherence to UDF patterns.
 *
 * @param owner The repository owner.
 * @param repoName The repository name.
 * @param onNavigateBack Callback invoked when the back navigation action is triggered.
 * @param onIssueCreated Callback invoked with the new issue number upon successful creation.
 * @param viewModel The [IssueViewModel] handling the creation logic.
 */
@Composable
fun RaiseIssueScreen(
    owner: String,
    repoName: String,
    onNavigateBack: () -> Unit,
    onIssueCreated: (issueNumber: Int) -> Unit,
    viewModel: IssueViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scaffoldState = rememberScaffoldState()
    val uiState by viewModel.uiState.collectAsState()

    // Show error messages in a Snackbar
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = errorMessage!!,
                duration = SnackbarDuration.Short
            )
            errorMessage = null
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.issue_create_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colors.onSurface
                        )
                    } else {
                        val isEnabled = title.isNotBlank() && !uiState.isLoading
                        IconButton(
                            onClick = {
                                viewModel.createIssue(
                                    owner = owner,
                                    repo = repoName,
                                    title = title,
                                    body = body,
                                    onSuccess = { issueNumber ->
                                        onIssueCreated(issueNumber)
                                    },
                                    onError = { error ->
                                        errorMessage = error
                                    }
                                )
                            },
                            enabled = isEnabled
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = stringResource(id = R.string.action_submit),
                                tint = if (isEnabled)
                                    MaterialTheme.colors.onSurface
                                else
                                    MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.issue_title_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !uiState.isLoading,
                singleLine = true
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.issue_comment_optional_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enabled = !uiState.isLoading,
                maxLines = Int.MAX_VALUE
            )
        }
    }
} 