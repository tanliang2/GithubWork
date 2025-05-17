package com.tl.githubcompose.data.model

/**
 * Represents the request body structure for creating a new GitHub Issue.
 */
data class ProblemRequestBody(
    val title: String,
    val body: String? = null
    // labels, assignees etc. can be added
)