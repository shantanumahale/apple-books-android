package com.applebooks.android.data.repository

import com.applebooks.android.data.local.db.dao.ReadingProgressDao
import com.applebooks.android.data.local.db.entity.ReadingProgressEntity
import com.applebooks.android.domain.model.ReadingProgress
import com.applebooks.android.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingProgressRepositoryImpl @Inject constructor(
    private val readingProgressDao: ReadingProgressDao
) : ReadingProgressRepository {

    override suspend fun getProgress(bookId: Long): ReadingProgress? =
        readingProgressDao.getProgress(bookId)?.toDomain()

    override fun getProgressFlow(bookId: Long): Flow<ReadingProgress?> =
        readingProgressDao.getProgressFlow(bookId).map { it?.toDomain() }

    override suspend fun saveProgress(progress: ReadingProgress) =
        readingProgressDao.upsert(progress.toEntity())

    private fun ReadingProgressEntity.toDomain() = ReadingProgress(
        bookId = bookId,
        locatorJson = locatorJson,
        progressPercent = progressPercent,
        lastReadAt = lastReadAt
    )

    private fun ReadingProgress.toEntity() = ReadingProgressEntity(
        bookId = bookId,
        locatorJson = locatorJson,
        progressPercent = progressPercent,
        lastReadAt = lastReadAt
    )
}
