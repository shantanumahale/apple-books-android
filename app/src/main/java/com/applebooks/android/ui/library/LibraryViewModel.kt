package com.applebooks.android.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.applebooks.android.data.local.preferences.UserPreferencesDataStore
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.model.BookFormat
import com.applebooks.android.domain.repository.BookRepository
import com.applebooks.android.util.CoverExtractor
import com.applebooks.android.util.FileMetadataExtractor
import com.applebooks.android.util.UriPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isGridView: Boolean = true,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    application: Application
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isAddingBook = MutableStateFlow(false)
    val isAddingBook: StateFlow<Boolean> = _isAddingBook.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                bookRepository.getAllBooksFlow()
            } else {
                bookRepository.searchBooksFlow(query.trim())
            }
        },
        userPreferencesDataStore.preferencesFlow
    ) { books, preferences ->
        LibraryUiState(
            books = books,
            isGridView = preferences.libraryGridView,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleGridView() {
        viewModelScope.launch {
            val currentGrid = uiState.value.isGridView
            userPreferencesDataStore.updateLibraryGridView(!currentGrid)
        }
    }

    fun addBookFromUri(
        uri: Uri,
        mimeType: String?,
        onResult: (bookId: Long?, navigateToFormat: BookFormat?) -> Unit
    ) {
        viewModelScope.launch {
            _isAddingBook.value = true
            try {
                val context = getApplication<Application>()

                UriPermissionManager.takePersistablePermission(context.contentResolver, uri)

                val existingBook = bookRepository.getBookByUri(uri.toString())
                if (existingBook != null) {
                    bookRepository.updateLastOpened(existingBook.id)
                    onResult(existingBook.id, existingBook.format)
                    return@launch
                }

                val metadata = FileMetadataExtractor.extract(context, uri, mimeType)
                if (metadata == null) {
                    UriPermissionManager.releasePersistablePermission(context.contentResolver, uri)
                    onResult(null, null)
                    return@launch
                }

                val book = Book(
                    title = metadata.title,
                    author = metadata.author,
                    fileUri = uri.toString(),
                    format = metadata.format,
                    totalPages = metadata.totalPages
                )

                val bookId = bookRepository.addBook(book)

                onResult(bookId, metadata.format)

                extractAndSaveCover(context, uri, bookId, metadata.format)
            } catch (_: Exception) {
                onResult(null, null)
            } finally {
                _isAddingBook.value = false
            }
        }
    }

    private suspend fun extractAndSaveCover(
        context: Application,
        uri: Uri,
        bookId: Long,
        format: BookFormat
    ) {
        try {
            val coverPath = when (format) {
                BookFormat.PDF -> CoverExtractor.extractPdfCover(context, uri, bookId)
                BookFormat.EPUB -> CoverExtractor.extractEpubCover(context, uri, bookId)
            }
            if (coverPath != null) {
                bookRepository.updateCoverPath(bookId, coverPath)
            }
        } catch (_: Exception) {
            // Cover extraction is non-critical; the book is already added
        }
    }

    suspend fun getBookById(bookId: Long): Book? = bookRepository.getBookById(bookId)

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            try {
                val book = bookRepository.getBookById(bookId) ?: return@launch

                book.coverCachePath?.let { CoverExtractor.deleteCover(it) }

                try {
                    val uri = Uri.parse(book.fileUri)
                    UriPermissionManager.releasePersistablePermission(
                        getApplication<Application>().contentResolver,
                        uri
                    )
                } catch (_: Exception) {
                    // URI may already be invalid
                }

                bookRepository.deleteBook(bookId)
            } catch (_: Exception) {
                // Deletion failed silently; could be extended with error state
            }
        }
    }
}
