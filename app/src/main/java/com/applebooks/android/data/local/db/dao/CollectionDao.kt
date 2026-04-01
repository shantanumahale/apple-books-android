package com.applebooks.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.applebooks.android.data.local.db.entity.BookCollectionCrossRef
import com.applebooks.android.data.local.db.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("""
        SELECT c.*, COUNT(bcr.bookId) as bookCount
        FROM collections c
        LEFT JOIN book_collection_cross_ref bcr ON c.id = bcr.collectionId
        GROUP BY c.id
        ORDER BY c.sortOrder ASC, c.createdAt DESC
    """)
    fun getAllCollectionsFlow(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: Long): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(crossRef: BookCollectionCrossRef)

    @Query("DELETE FROM book_collection_cross_ref WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBookFromCollection(bookId: Long, collectionId: Long)

    @Query("SELECT collectionId FROM book_collection_cross_ref WHERE bookId = :bookId")
    suspend fun getCollectionIdsForBook(bookId: Long): List<Long>

    data class CollectionWithCount(
        val id: Long,
        val name: String,
        val createdAt: Long,
        val sortOrder: Int,
        val bookCount: Int
    )
}
