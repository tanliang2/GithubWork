package com.tl.githubcompose.ui.repository

import android.util.Base64
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.tl.githubcompose.R
import com.tl.githubcompose.ui.components.ErrorRetry
import java.nio.charset.Charset

/**
 * Composable function for the Repository Detail screen.
 * Displays repository information (name, description, stats) and its README content.
 *
 * @param owner The owner login of the repository.
 * @param repoName The name of the repository.
 * @param viewModel The [RepositoryViewModel] providing the UI state and loading logic.
 * @param onNavigateBack Callback invoked when the back navigation action is triggered.
 * @param onNavigateToRaiseIssue Callback invoked when the action to navigate to the raise issue screen is triggered.
 * @param onNavigateToIssues Callback invoked when the action to navigate to the issues list screen is triggered.
 */
@Composable
fun RepositoryScreen(
    owner: String,
    repoName: String,
    viewModel: RepositoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRaiseIssue: (owner: String, repoName: String) -> Unit = { _, _ -> },
    onNavigateToIssues: (owner: String, repoName: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(owner, repoName) {
        // Load details when owner/repoName changes
        viewModel.loadRepositoryDetails(owner, repoName)
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(repoName, color = Color.Black) },
                backgroundColor = Color.White,
                elevation = 0.dp, // Flat top bar
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back),
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    // Action to navigate to the Issues list
                    TextButton(
                        onClick = { onNavigateToIssues(owner, repoName) }
                    ) {
                        Text(stringResource(id = R.string.repository_issues_button), color = MaterialTheme.colors.primary)
                    }
                    // Action to navigate to the screen for creating a new issue
                    TextButton(
                        onClick = { onNavigateToRaiseIssue(owner, repoName) }
                    ) {
                        Text(stringResource(id = R.string.repository_new_issue_button), color = MaterialTheme.colors.primary)
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
                // Loading state
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // Error state
                uiState.error != null -> {
                    ErrorRetry(
                        message = uiState.error ?: stringResource(id = R.string.error_unknown),
                        onRetry = { viewModel.loadRepositoryDetails(owner, repoName) }
                    )
                }
                // Success state with repository data
                uiState.repository != null -> {
                    RepositoryContent(uiState = uiState)
                }
                // Fallback state (should ideally not be reached if loading/error/success covers all)
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                         Text(stringResource(id = R.string.error_repo_load_failed))
                    }
                }
            }
        }
    }
}

/**
 * Composable function displaying the main content of the repository details,
 * including metadata and README.
 *
 * @param uiState The current [RepositoryUiState] containing repository and README data.
 */
@Composable
fun RepositoryContent(uiState: RepositoryUiState) {
    // Non-null assertion is safe here due to the check in the calling composable
    val repo = uiState.repository!!
    val scrollState = rememberScrollState()
    var decodedReadme by remember { mutableStateOf<String?>(null) }
    var isMarkdownVisible by remember { mutableStateOf(false) }

    // Decode the Base64 encoded README content when it becomes available or changes.
    LaunchedEffect(uiState.readmeContent) {
        decodedReadme = uiState.readmeContent?.let {
            try {
                // Decode Base64, removing potential newlines first
                String(Base64.decode(it.replace("\n", ""), Base64.DEFAULT), Charset.forName("UTF-8"))
            } catch (e: Exception) {
                Log.e("RepositoryScreen", "Failed to decode README content", e)
                "Failed to decode README: ${e.message}" // English error message
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(text = repo.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        repo.description?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.body1, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = stringResource(id = R.string.repo_item_stars_description), tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${repo.stargazersCount}", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Default.ForkRight, contentDescription = stringResource(id = R.string.repo_item_forks_description), tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${repo.forksCount}", color = Color.Gray, fontSize = 14.sp)
            repo.language?.let {
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Code, contentDescription = stringResource(id = R.string.repo_item_language_description), tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = it, color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Render Markdown README with lazy loading
        if (decodedReadme != null) {
            // Show a button to toggle markdown visibility
            if (!isMarkdownVisible) {
                TextButton(
                    onClick = { isMarkdownVisible = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.repository_show_readme))
                }
            } else {
                // Remember the potentially large decoded content to avoid re-processing on minor recompositions.
                // Limit the content size to prevent performance issues with very large READMEs.
                val markdownContent = remember(decodedReadme) {
                    decodedReadme!!.take(10000) // Limit content size to 10k chars
                }
                
                Markdown(
                    content = markdownContent,
                    typography = markdownTypography(),
                    colors = markdownColor(),
                    imageTransformer = Coil3ImageTransformerImpl
                )
            }
        } else if (uiState.readmeContent == null && !uiState.isLoading) {
            // Display message if README is confirmed not found (and not just loading)
            Text(stringResource(id = R.string.repository_readme_not_found), color = Color.Gray)
        }
    }
}

/**
 * Provides custom typography settings for the Markdown renderer.
 * Uses MaterialTheme typography as a base.
 * (Consider using library defaults if no customization is needed)
 */
@Composable
fun markdownTypography(
    h1: TextStyle = MaterialTheme.typography.h4,
    h2: TextStyle = MaterialTheme.typography.h5,
    h3: TextStyle = MaterialTheme.typography.h6,
    h4: TextStyle = MaterialTheme.typography.h4,
    h5: TextStyle = MaterialTheme.typography.h5,
    h6: TextStyle = MaterialTheme.typography.h6,
    text: TextStyle = MaterialTheme.typography.body1,
    code: TextStyle = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace),
    inlineCode: TextStyle = text.copy(fontFamily = FontFamily.Monospace),
    quote: TextStyle = MaterialTheme.typography.body2.plus(SpanStyle(fontStyle = FontStyle.Italic)),
    paragraph: TextStyle = MaterialTheme.typography.body1,
    ordered: TextStyle = MaterialTheme.typography.body1,
    bullet: TextStyle = MaterialTheme.typography.body1,
    list: TextStyle = MaterialTheme.typography.body1,
    link: TextStyle = MaterialTheme.typography.body1.copy(
        fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline
    ),
    textLink: TextLinkStyles = TextLinkStyles(style = link.toSpanStyle()),
    table: TextStyle = text,
): MarkdownTypography = DefaultMarkdownTypography(
    h1 = h1,
    h2 = h2,
    h3 = h3,
    h4 = h4,
    h5 = h5,
    h6 = h6,
    text = text,
    quote = quote,
    code = code,
    inlineCode = inlineCode,
    paragraph = paragraph,
    ordered = ordered,
    bullet = bullet,
    list = list,
    link = link,
    textLink = textLink,
    table = table,
)

/**
 * Provides custom color settings for the Markdown renderer.
 * Uses MaterialTheme colors as a base.
 * (Consider using library defaults if no customization is needed)
 */
@Composable
fun markdownColor(
    text: Color = MaterialTheme.colors.onBackground,
    codeText: Color = Color.Unspecified,
    inlineCodeText: Color = Color.Unspecified,
    linkText: Color = Color.Unspecified,
    codeBackground: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
    inlineCodeBackground: Color = codeBackground,
    dividerColor: Color = MaterialTheme.colors.primaryVariant,
    tableText: Color = Color.Unspecified,
    tableBackground: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.02f),
): MarkdownColors = DefaultMarkdownColors(
    text = text,
    codeText = codeText,
    inlineCodeText = inlineCodeText,
    linkText = linkText,
    codeBackground = codeBackground,
    inlineCodeBackground = inlineCodeBackground,
    dividerColor = dividerColor,
    tableText = tableText,
    tableBackground = tableBackground,
)