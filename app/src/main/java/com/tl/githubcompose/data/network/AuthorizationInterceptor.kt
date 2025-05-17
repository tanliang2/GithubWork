package com.tl.githubcompose.data.network

import com.tl.githubcompose.data.DataStoreHelper
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An OkHttp Interceptor that adds the required `Accept` header and the
 * `Authorization` header with the bearer token (if available) to GitHub API requests.
 */
@Singleton // Mark as Singleton if it should be reused
class AuthorizationInterceptor @Inject constructor(
    private val dataStoreHelper: DataStoreHelper // Inject DataStoreHelper
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/vnd.github+json") // Use .header() to overwrite if already present

        dataStoreHelper.getToken()?.let {
            requestBuilder.header("Authorization", "token $it") // Use .header() for consistency
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
