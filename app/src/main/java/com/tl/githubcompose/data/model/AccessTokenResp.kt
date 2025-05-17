package com.tl.githubcompose.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the response structure when exchanging an authorization code for an access token.
 */
data class AccessTokenResp(

    @SerializedName("access_token")
    val accessToken: String,
    val scope: String?,
    @SerializedName("token_type")
    val tokenType: String?
)