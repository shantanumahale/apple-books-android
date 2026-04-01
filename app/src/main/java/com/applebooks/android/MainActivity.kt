package com.applebooks.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.applebooks.android.domain.model.BookFormat
import com.applebooks.android.domain.repository.BookRepository
import com.applebooks.android.ui.navigation.AppNavGraph
import com.applebooks.android.ui.theme.AppleBooksTheme
import com.applebooks.android.util.CoverExtractor
import com.applebooks.android.util.FileMetadataExtractor
import com.applebooks.android.util.UriPermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bookRepository: BookRepository

    private var initialBookId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            AppleBooksTheme {
                AppNavGraph(initialBookId = initialBookId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return

        lifecycleScope.launch {
            val bookId = addBookFromIntent(uri, intent.type)
            if (bookId != null) {
                initialBookId = bookId
            }
        }
    }

    private suspend fun addBookFromIntent(uri: Uri, mimeType: String?): Long? {
        return try {
            UriPermissionManager.takePersistablePermission(contentResolver, uri)

            val existingBook = bookRepository.getBookByUri(uri.toString())
            if (existingBook != null) {
                bookRepository.updateLastOpened(existingBook.id)
                return existingBook.id
            }

            val metadata = FileMetadataExtractor.extract(this, uri, mimeType) ?: return null

            val book = com.applebooks.android.domain.model.Book(
                title = metadata.title,
                author = metadata.author,
                fileUri = uri.toString(),
                format = metadata.format,
                totalPages = metadata.totalPages
            )

            val bookId = bookRepository.addBook(book)

            // Extract cover in background
            val coverPath = when (metadata.format) {
                BookFormat.PDF -> CoverExtractor.extractPdfCover(this, uri, bookId)
                BookFormat.EPUB -> CoverExtractor.extractEpubCover(this, uri, bookId)
            }
            if (coverPath != null) {
                bookRepository.updateCoverPath(bookId, coverPath)
            }

            bookId
        } catch (_: Exception) {
            null
        }
    }
}
