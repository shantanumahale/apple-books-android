package com.applebooks.android.domain.repository

import com.applebooks.android.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface ReadingProgressRepository {
    suspend fun getProgress(bookId: Long): ReadingProgress?
    fun getProgressFlow(bookId: Long): Flow<ReadingProgress?>
    suspend fun saveProgress(progress: ReadingProgress)
}
