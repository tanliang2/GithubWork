package com.tl.githubcompose.di

import com.tl.githubcompose.data.DataStore
import com.tl.githubcompose.data.DataStoreHelper
import com.tl.githubcompose.data.GithubRepository
import com.tl.githubcompose.data.GithubRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGithubRepository(impl: GithubRepositoryImpl): GithubRepository

    @Binds
    @Singleton
    abstract fun bindDataStore(impl: DataStoreHelper): DataStore
} 