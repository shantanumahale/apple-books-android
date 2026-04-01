package com.applebooks.android.data.repository

import com.applebooks.android.data.local.db.dao.CollectionDao
import com.applebooks.android.data.local.db.entity.BookCollectionCrossRef
import com.applebooks.android.data.local.db.entity.CollectionEntity
import com.applebooks.android.domain.model.Collection
import com.applebooks.android.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao
) : CollectionRepository {

    override fun getAllCollectionsFlow(): Flow<List<Collection>> =
        collectionDao.getAllCollectionsFlow().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getCollectionById(id: Long): Collection? =
        collectionDao.getCollectionById(id)?.let {
            Collection(id = it.id, name = it.name, createdAt = it.createdAt, sortOrder = it.sortOrder)
        }

    override suspend fun createCollection(name: String): Long =
        collectionDao.insert(CollectionEntity(name = name))

    override suspend fun renameCollection(id: Long, name: String) =
        collectionDao.rename(id, name)

    override suspend fun deleteCollection(id: Long) =
        collectionDao.deleteById(id)

    override suspend fun addBookToCollection(bookId: Long, collectionId: Long) =
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId))

    override suspend fun removeBookFromCollection(bookId: Long, collectionId: Long) =
        collectionDao.removeBookFromCollection(bookId, collectionId)

    override suspend fun getCollectionIdsForBook(bookId: Long): List<Long> =
        collectionDao.getCollectionIdsForBook(bookId)

    private fun CollectionDao.CollectionWithCount.toDomain() = Collection(
        id = id,
        name = name,
        createdAt = createdAt,
        sortOrder = sortOrder,
        bookCount = bookCount
    )
}
