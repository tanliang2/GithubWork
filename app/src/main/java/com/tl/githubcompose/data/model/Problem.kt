package com.tl.githubcompose.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a GitHub Issue.
 */
data class Problem(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String,
    val body: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val user: User
)