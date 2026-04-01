package com.applebooks.android.domain.repository

import com.applebooks.android.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooksFlow(): Flow<List<Book>>
    fun searchBooksFlow(query: String): Flow<List<Book>>
    fun getBooksInCollectionFlow(collectionId: Long): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    suspend fun getBookByUri(uri: String): Book?
    suspend fun addBook(book: Book): Long
    suspend fun updateLastOpened(bookId: Long)
    suspend fun updateCoverPath(bookId: Long, path: String)
    suspend fun deleteBook(bookId: Long)
}
