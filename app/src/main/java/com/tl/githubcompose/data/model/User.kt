package com.tl.githubcompose.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a GitHub User.
 */
data class User(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @SerializedName("html_url")
    val htmlUrl: String? = null,
    val name: String? = null,
    val company: String? = null,
    val blog: String? = null,
    val location: String? = null,
    val email: String? = null,
    val bio: String? = null,
    val followers: Int? = null,
    val following: Int? = null
    // Add other fields as needed (e.g., public_repos, created_at)
)