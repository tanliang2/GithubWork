package com.tl.githubcompose.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a GitHub Repository.
 */
data class Repository(
    val id: Long,
    val name: String,
    @SerializedName("full_name")
    val fullName: String,
    val owner: User,
    val description: String?,
    @SerializedName("stargazers_count")
    val stargazersCount: Int,
    @SerializedName("forks_count")
    val forksCount: Int,
    val language: String?,
    @SerializedName("html_url")
    val htmlUrl: String
)