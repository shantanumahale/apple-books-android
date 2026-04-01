package com.applebooks.android.domain.repository

import com.applebooks.android.domain.model.Collection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getAllCollectionsFlow(): Flow<List<Collection>>
    suspend fun getCollectionById(id: Long): Collection?
    suspend fun createCollection(name: String): Long
    suspend fun renameCollection(id: Long, name: String)
    suspend fun deleteCollection(id: Long)
    suspend fun addBookToCollection(bookId: Long, collectionId: Long)
    suspend fun removeBookFromCollection(bookId: Long, collectionId: Long)
    suspend fun getCollectionIdsForBook(bookId: Long): List<Long>
}
