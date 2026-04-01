package com.applebooks.android.data.repository

import com.applebooks.android.data.local.db.dao.BookDao
import com.applebooks.android.data.local.db.entity.BookEntity
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.model.BookFormat
import com.applebooks.android.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAllBooksFlow(): Flow<List<Book>> =
        bookDao.getAllBooksFlow().map { list -> list.map { it.toDomain() } }

    override fun searchBooksFlow(query: String): Flow<List<Book>> =
        bookDao.searchBooksFlow(query).map { list -> list.map { it.toDomain() } }

    override fun getBooksInCollectionFlow(collectionId: Long): Flow<List<Book>> =
        bookDao.getBooksInCollectionFlow(collectionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getBookById(id: Long): Book? =
        bookDao.getBookById(id)?.toDomain()

    override suspend fun getBookByUri(uri: String): Book? =
        bookDao.getBookByUri(uri)?.toDomain()

    override suspend fun addBook(book: Book): Long =
        bookDao.insert(book.toEntity())

    override suspend fun updateLastOpened(bookId: Long) =
        bookDao.updateLastOpened(bookId)

    override suspend fun updateCoverPath(bookId: Long, path: String) =
        bookDao.updateCoverPath(bookId, path)

    override suspend fun deleteBook(bookId: Long) =
        bookDao.deleteById(bookId)

    private fun BookDao.BookWithProgress.toDomain() = Book(
        id = id,
        title = title,
        author = author,
        fileUri = fileUri,
        coverCachePath = coverCachePath,
        format = try { BookFormat.valueOf(format) } catch (_: Exception) { BookFormat.PDF },
        totalPages = totalPages,
        addedAt = addedAt,
        lastOpenedAt = lastOpenedAt,
        progressPercent = progress
    )

    private fun BookEntity.toDomain() = Book(
        id = id,
        title = title,
        author = author,
        fileUri = fileUri,
        coverCachePath = coverCachePath,
        format = try { BookFormat.valueOf(format) } catch (_: Exception) { BookFormat.PDF },
        totalPages = totalPages,
        addedAt = addedAt,
        lastOpenedAt = lastOpenedAt
    )

    private fun Book.toEntity() = BookEntity(
        id = id,
        title = title,
        author = author,
        fileUri = fileUri,
        coverCachePath = coverCachePath,
        format = format.name,
        totalPages = totalPages,
        addedAt = addedAt,
        lastOpenedAt = lastOpenedAt
    )
}
