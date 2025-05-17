package com.tl.githubcompose.data.model

import com.google.gson.annotations.SerializedName


data class Readme(
    @SerializedName("content") val content: String?
    // Add other fields if needed, like 'encoding', 'size', etc.
) 