package com.applebooks.android.di

import android.content.Context
import androidx.room.Room
import com.applebooks.android.data.local.db.AppDatabase
import com.applebooks.android.data.local.db.dao.BookDao
import com.applebooks.android.data.local.db.dao.CollectionDao
import com.applebooks.android.data.local.db.dao.ReadingProgressDao
import com.applebooks.android.data.local.preferences.UserPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "applebooks.db"
        ).build()

    @Provides
    fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideReadingProgressDao(db: AppDatabase): ReadingProgressDao = db.readingProgressDao()

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(@ApplicationContext context: Context): UserPreferencesDataStore =
        UserPreferencesDataStore(context)
}
