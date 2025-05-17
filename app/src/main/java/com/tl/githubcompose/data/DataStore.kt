package com.tl.githubcompose.data

interface DataStore {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
} 