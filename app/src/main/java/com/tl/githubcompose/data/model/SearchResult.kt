package com.tl.githubcompose.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the response structure for a GitHub repository search API call.
 */
data class SearchResult(
    val totalCount: Int,
    @SerializedName("incomplete_results")
    val incompleteResults: Boolean,
    val items: List<Repository>
)