package com.tl.githubcompose.di

import com.tl.githubcompose.data.network.AuthorizationInterceptor
import com.tl.githubcompose.data.network.GithubApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Hilt module that provides network-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "https://api.github.com/"

    /**
     * Provides a singleton instance of [OkHttpClient].
     *
     * Includes an [HttpLoggingInterceptor] for logging network requests/responses
     * and an [AuthorizationInterceptor] to add the necessary auth headers.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authorizationInterceptor: AuthorizationInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addInterceptor(authorizationInterceptor)
            .build()
    }

    /**
     * Provides a singleton instance of [Retrofit].
     *
     * @param okHttpClient The [OkHttpClient] instance to use for network requests.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // Use Gson or Moshi
            .build()
    }

    /**
     * Provides a singleton instance of [GithubApiService].
     *
     * @param retrofit The [Retrofit] instance used to create the service.
     */
    @Provides
    @Singleton
    fun provideGithubApiService(retrofit: Retrofit): GithubApiService {
        return retrofit.create(GithubApiService::class.java)
    }

}