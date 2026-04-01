package com.applebooks.android.reader.epub

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applebooks.android.data.local.preferences.UserPreferencesDataStore
import com.applebooks.android.domain.model.Book
import com.applebooks.android.domain.model.PageTurnEffect
import com.applebooks.android.domain.model.ReaderFont
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
import org.json.JSONObject
import javax.inject.Inject

data class EpubReaderState(
    val book: Book? = null,
    val epubBook: EpubBook? = null,
    val currentChapterIndex: Int = 0,
    val scrollPosition: Float = 0f,
    val readingTheme: ReadingTheme = ReadingTheme.WHITE,
    val readerFont: ReaderFont = ReaderFont.SYSTEM_SERIF,
    val fontSize: Int = 18,
    val pageTurnEffect: PageTurnEffect = PageTurnEffect.SLIDE,
    val showOverlay: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val totalChapters: Int get() = epubBook?.chapters?.size ?: 0
    val currentChapterTitle: String get() = epubBook?.chapters?.getOrNull(currentChapterIndex)?.title ?: ""
    val tableOfContents: List<TocEntry> get() = epubBook?.tableOfContents ?: emptyList()
}

@HiltViewModel
class EpubReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val readingProgressRepository: ReadingProgressRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val application: Application
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    private val _state = MutableStateFlow(EpubReaderState())
    val state: StateFlow<EpubReaderState> = _state.asStateFlow()

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
                        readerFont = prefs.readerFont,
                        fontSize = prefs.fontSize,
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
                val epubBook = withContext(Dispatchers.IO) {
                    val parser = EpubParser(application.contentResolver)
                    parser.parse(Uri.parse(book.fileUri))
                }

                if (epubBook.chapters.isEmpty()) {
                    _state.update { it.copy(isLoading = false, error = "EPUB contains no readable chapters") }
                    return@launch
                }

                val progress = readingProgressRepository.getProgress(bookId)
                val restoredChapterIndex: Int
                val restoredScrollPosition: Float

                if (progress != null && progress.locatorJson.isNotEmpty()) {
                    try {
                        val json = JSONObject(progress.locatorJson)
                        restoredChapterIndex = json.optInt("chapterIndex", 0)
                            .coerceIn(0, epubBook.chapters.size - 1)
                        restoredScrollPosition = json.optDouble("scrollPosition", 0.0).toFloat()
                    } catch (_: Exception) {
                        restoredChapterIndex = 0
                        restoredScrollPosition = 0f
                    }
                } else {
                    restoredChapterIndex = 0
                    restoredScrollPosition = 0f
                }

                _state.update {
                    it.copy(
                        book = book,
                        epubBook = epubBook,
                        currentChapterIndex = restoredChapterIndex,
                        scrollPosition = restoredScrollPosition,
                        isLoading = false
                    )
                }
            } catch (e: EpubParseException) {
                _state.update {
                    it.copy(isLoading = false, error = "Failed to parse EPUB: ${e.message}")
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Failed to open EPUB: ${e.localizedMessage}")
                }
            }
        }
    }

    fun goToChapter(index: Int) {
        val totalChapters = _state.value.totalChapters
        if (totalChapters <= 0) return
        val clampedIndex = index.coerceIn(0, totalChapters - 1)
        _state.update {
            it.copy(
                currentChapterIndex = clampedIndex,
                scrollPosition = 0f
            )
        }
        saveProgress()
    }

    fun nextChapter() {
        val current = _state.value.currentChapterIndex
        val total = _state.value.totalChapters
        if (current < total - 1) {
            goToChapter(current + 1)
        }
    }

    fun previousChapter() {
        val current = _state.value.currentChapterIndex
        if (current > 0) {
            goToChapter(current - 1)
        }
    }

    fun toggleOverlay() {
        _state.update { it.copy(showOverlay = !it.showOverlay) }
    }

    fun updateScrollPosition(position: Float) {
        _state.update { it.copy(scrollPosition = position) }
    }

    fun saveProgress() {
        val currentState = _state.value
        if (currentState.totalChapters <= 0) return

        val progressPercent = if (currentState.totalChapters > 1) {
            (currentState.currentChapterIndex.toFloat() + currentState.scrollPosition) /
                    currentState.totalChapters.toFloat()
        } else {
            currentState.scrollPosition
        }

        val locatorJson = JSONObject().apply {
            put("chapterIndex", currentState.currentChapterIndex)
            put("scrollPosition", currentState.scrollPosition.toDouble())
        }.toString()

        viewModelScope.launch {
            readingProgressRepository.saveProgress(
                ReadingProgress(
                    bookId = bookId,
                    locatorJson = locatorJson,
                    progressPercent = progressPercent.coerceIn(0f, 1f),
                    lastReadAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateReadingTheme(theme: ReadingTheme) {
        viewModelScope.launch {
            userPreferencesDataStore.updateReadingTheme(theme)
        }
    }

    fun updateReaderFont(font: ReaderFont) {
        viewModelScope.launch {
            userPreferencesDataStore.updateReaderFont(font)
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            userPreferencesDataStore.updateFontSize(size.coerceIn(12, 32))
        }
    }

    fun updatePageTurnEffect(effect: PageTurnEffect) {
        viewModelScope.launch {
            userPreferencesDataStore.updatePageTurnEffect(effect)
        }
    }

    fun getChapterHtml(chapterIndex: Int): String? {
        return _state.value.epubBook?.chapters?.getOrNull(chapterIndex)?.htmlContent
    }

    fun buildStyledHtml(rawHtml: String, theme: ReadingTheme, font: ReaderFont, fontSize: Int): String {
        val bgHex = colorToHex(theme.backgroundColor)
        val textHex = colorToHex(theme.textColor)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>
                    * {
                        box-sizing: border-box;
                    }
                    html, body {
                        margin: 0;
                        padding: 0;
                        background-color: $bgHex !important;
                        color: $textHex !important;
                        font-family: ${font.cssName} !important;
                        font-size: ${fontSize}px !important;
                        line-height: 1.6 !important;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                        -webkit-text-size-adjust: none;
                    }
                    body {
                        padding: 16px 20px 60px 20px;
                    }
                    p, div, span, li, td, th, blockquote, pre, h1, h2, h3, h4, h5, h6 {
                        color: $textHex !important;
                    }
                    a {
                        color: #4A90D9 !important;
                    }
                    img {
                        max-width: 100% !important;
                        height: auto !important;
                    }
                    table {
                        max-width: 100% !important;
                        overflow-x: auto;
                    }
                    pre, code {
                        white-space: pre-wrap !important;
                        word-wrap: break-word !important;
                    }
                </style>
                <script>
                    let lastReportedProgress = -1;

                    function getScrollProgress() {
                        var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                        var scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
                        if (scrollHeight <= 0) return 0;
                        return Math.min(1.0, scrollTop / scrollHeight);
                    }

                    function reportScroll() {
                        var progress = getScrollProgress();
                        var rounded = Math.round(progress * 1000) / 1000;
                        if (rounded !== lastReportedProgress) {
                            lastReportedProgress = rounded;
                            if (window.EpubBridge) {
                                window.EpubBridge.onScrollChanged(rounded);
                            }
                        }
                    }

                    function scrollToProgress(progress) {
                        var scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
                        window.scrollTo(0, scrollHeight * progress);
                    }

                    function isAtBottom() {
                        var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                        var scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
                        return scrollTop >= scrollHeight - 5;
                    }

                    function isAtTop() {
                        var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                        return scrollTop <= 5;
                    }

                    window.addEventListener('scroll', function() {
                        reportScroll();
                    });

                    document.addEventListener('DOMContentLoaded', function() {
                        reportScroll();
                    });
                </script>
            </head>
            <body>
                $rawHtml
            </body>
            </html>
        """.trimIndent()
    }

    private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        return String.format("#%02X%02X%02X", r, g, b)
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
    }
}
