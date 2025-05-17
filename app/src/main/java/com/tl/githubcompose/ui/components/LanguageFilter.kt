package com.tl.githubcompose.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tl.githubcompose.R

/**
 * A composable function that displays a horizontal list of programming languages
 * as filter chips. Allows users to select a language or view all.
 *
 * @param selectedLanguage The currently selected language (null means "All").
 * @param onLanguageSelected Callback invoked when a language chip (or "All") is clicked.
 * @param modifier Optional modifier for this composable.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LanguageFilter(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf(
        "Java",
        "Python",
        "Rust",
        "Kotlin",
        "Go",
        "JavaScript",
        "TypeScript",
        "Swift",
        "C++",
        "C#"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedLanguage == null,
                onClick = { onLanguageSelected(null) },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(stringResource(id = R.string.language_filter_all))
            }
        }
        items(languages) { language ->
            FilterChip(
                selected = selectedLanguage == language,
                onClick = { onLanguageSelected(language) },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(language)
            }
        }
    }
} 