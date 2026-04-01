package com.applebooks.android.di

import com.applebooks.android.data.repository.BookRepositoryImpl
import com.applebooks.android.data.repository.CollectionRepositoryImpl
import com.applebooks.android.data.repository.ReadingProgressRepositoryImpl
import com.applebooks.android.domain.repository.BookRepository
import com.applebooks.android.domain.repository.CollectionRepository
import com.applebooks.android.domain.repository.ReadingProgressRepository
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
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    @Binds
    @Singleton
    abstract fun bindReadingProgressRepository(impl: ReadingProgressRepositoryImpl): ReadingProgressRepository
}
