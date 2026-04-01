package com.applebooks.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.applebooks.android.data.local.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("""
        SELECT b.*, COALESCE(rp.progressPercent, 0) as progress
        FROM books b
        LEFT JOIN reading_progress rp ON b.id = rp.bookId
        ORDER BY b.lastOpenedAt DESC, b.addedAt DESC
    """)
    fun getAllBooksFlow(): Flow<List<BookWithProgress>>

    @Query("""
        SELECT b.*, COALESCE(rp.progressPercent, 0) as progress
        FROM books b
        LEFT JOIN reading_progress rp ON b.id = rp.bookId
        WHERE b.title LIKE '%' || :query || '%' OR b.author LIKE '%' || :query || '%'
        ORDER BY b.lastOpenedAt DESC, b.addedAt DESC
    """)
    fun searchBooksFlow(query: String): Flow<List<BookWithProgress>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE fileUri = :uri")
    suspend fun getBookByUri(uri: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("UPDATE books SET lastOpenedAt = :timestamp WHERE id = :bookId")
    suspend fun updateLastOpened(bookId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET coverCachePath = :path WHERE id = :bookId")
    suspend fun updateCoverPath(bookId: Long, path: String)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteById(bookId: Long)

    @Query("""
        SELECT b.*, COALESCE(rp.progressPercent, 0) as progress
        FROM books b
        LEFT JOIN reading_progress rp ON b.id = rp.bookId
        INNER JOIN book_collection_cross_ref bcr ON b.id = bcr.bookId
        WHERE bcr.collectionId = :collectionId
        ORDER BY b.title ASC
    """)
    fun getBooksInCollectionFlow(collectionId: Long): Flow<List<BookWithProgress>>

    data class BookWithProgress(
        val id: Long,
        val title: String,
        val author: String?,
        val fileUri: String,
        val coverCachePath: String?,
        val format: String,
        val totalPages: Int?,
        val addedAt: Long,
        val lastOpenedAt: Long?,
        val progress: Float
    )
}
