package com.applebooks.android.reader.pdf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applebooks.android.data.local.preferences.UserPreferencesDataStore
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.model.PageTurnEffect
import com.applebooks.android.domain.model.ReadingProgress
import com.applebooks.android.domain.model.ReadingTheme
import com.applebooks.android.domain.repository.BookRepository
import com.applebooks.android.domain.repository.ReadingProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PdfReaderState(
    val book: Book? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val readingTheme: ReadingTheme = ReadingTheme.WHITE,
    val pageTurnEffect: PageTurnEffect = PageTurnEffect.SLIDE,
    val showOverlay: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val readingProgressRepository: ReadingProgressRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val application: Application
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    private val _state = MutableStateFlow(PdfReaderState())
    val state: StateFlow<PdfReaderState> = _state.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private val bitmapCache = PdfBitmapCache()

    private var screenDensity: Float = application.resources.displayMetrics.density
    private var screenWidth: Int = application.resources.displayMetrics.widthPixels
    private var screenHeight: Int = application.resources.displayMetrics.heightPixels

    init {
        observePreferences()
        loadBook()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesDataStore.preferencesFlow.collect { prefs ->
                _state.update {
                    it.copy(
                        readingTheme = prefs.readingTheme,
                        pageTurnEffect = prefs.pageTurnEffect
                    )
                }
            }
        }
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            if (book == null) {
                _state.update { it.copy(isLoading = false, error = "Book not found") }
                return@launch
            }

            bookRepository.updateLastOpened(bookId)

            try {
                val fileDescriptor = application.contentResolver
                    .openFileDescriptor(Uri.parse(book.fileUri), "r")

                if (fileDescriptor == null) {
                    _state.update { it.copy(isLoading = false, error = "Cannot open PDF file") }
                    return@launch
                }

                val renderer = PdfRenderer(fileDescriptor)
                pdfRenderer = renderer

                val totalPages = renderer.pageCount

                val progress = readingProgressRepository.getProgress(bookId)
                val restoredPage = progress?.locatorJson?.toIntOrNull()?.coerceIn(0, totalPages - 1) ?: 0

                _state.update {
                    it.copy(
                        book = book,
                        totalPages = totalPages,
                        currentPage = restoredPage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Failed to open PDF: ${e.localizedMessage}")
                }
            }
        }
    }

    suspend fun renderPage(pageIndex: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= _state.value.totalPages) return null

        bitmapCache.get(pageIndex)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val renderer = pdfRenderer ?: return@withContext null
                synchronized(renderer) {
                    val page = renderer.openPage(pageIndex)

                    val pageAspectRatio = page.width.toFloat() / page.height.toFloat()
                    val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

                    val bitmapWidth: Int
                    val bitmapHeight: Int

                    if (pageAspectRatio > screenAspectRatio) {
                        bitmapWidth = screenWidth
                        bitmapHeight = (screenWidth / pageAspectRatio).toInt()
                    } else {
                        bitmapHeight = screenHeight
                        bitmapWidth = (screenHeight * pageAspectRatio).toInt()
                    }

                    val bitmap = Bitmap.createBitmap(
                        bitmapWidth.coerceAtLeast(1),
                        bitmapHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                    page.close()

                    bitmapCache.put(pageIndex, bitmap)
                    bitmap
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun goToPage(page: Int) {
        val clampedPage = page.coerceIn(0, (_state.value.totalPages - 1).coerceAtLeast(0))
        _state.update { it.copy(currentPage = clampedPage) }
        saveProgress()
    }

    fun toggleOverlay() {
        _state.update { it.copy(showOverlay = !it.showOverlay) }
    }

    fun updateReadingTheme(theme: ReadingTheme) {
        viewModelScope.launch {
            userPreferencesDataStore.updateReadingTheme(theme)
        }
    }

    fun updatePageTurnEffect(effect: PageTurnEffect) {
        viewModelScope.launch {
            userPreferencesDataStore.updatePageTurnEffect(effect)
        }
    }

    fun saveProgress() {
        val currentState = _state.value
        if (currentState.totalPages <= 0) return

        val progressPercent = if (currentState.totalPages > 1) {
            currentState.currentPage.toFloat() / (currentState.totalPages - 1).toFloat()
        } else {
            1f
        }

        viewModelScope.launch {
            readingProgressRepository.saveProgress(
                ReadingProgress(
                    bookId = bookId,
                    locatorJson = currentState.currentPage.toString(),
                    progressPercent = progressPercent,
                    lastReadAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun preloadAdjacentPages(currentPage: Int) {
        viewModelScope.launch {
            val pagesToPreload = listOf(currentPage - 1, currentPage + 1)
            for (page in pagesToPreload) {
                if (page in 0 until _state.value.totalPages) {
                    renderPage(page)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {
        }
        pdfRenderer = null
        bitmapCache.clear()
    }
}
