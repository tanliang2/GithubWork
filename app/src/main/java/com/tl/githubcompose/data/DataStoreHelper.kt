package com.tl.githubcompose.data

import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A helper class for managing persistent data storage using MMKV.
 * Now injectable via Hilt.
 */
@Singleton
class DataStoreHelper @Inject constructor() : DataStore {

    companion object {
        private const val AUTH_TOKEN_KEY = "auth_token"
        private val mmkvInstance = MMKV.defaultMMKV()
    }

    /**
     * Saves the GitHub authentication token to MMKV.
     *
     * @param token The token string to save.
     */
    override fun saveToken(token: String) {
        mmkvInstance.encode(AUTH_TOKEN_KEY, token)
    }

    /**
     * Retrieves the saved GitHub authentication token from MMKV.
     *
     * @return The saved token string, or null if no token is found.
     */
    override fun getToken(): String? {
        return mmkvInstance.decodeString(AUTH_TOKEN_KEY, null)
    }

    /**
     * Removes the saved GitHub authentication token from MMKV.
     */
    override fun clearToken() {
        mmkvInstance.remove(AUTH_TOKEN_KEY)
    }
}