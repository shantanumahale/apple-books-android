package com.applebooks.android.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.applebooks.android.domain.model.BookFormat

data class BookMetadata(
    val title: String,
    val author: String? = null,
    val format: BookFormat,
    val totalPages: Int? = null
)

object FileMetadataExtractor {

    fun extract(context: Context, uri: Uri, mimeType: String?): BookMetadata? {
        val format = when {
            mimeType == "application/pdf" -> BookFormat.PDF
            mimeType == "application/epub+zip" -> BookFormat.EPUB
            uri.toString().endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
            uri.toString().endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
            else -> return null
        }

        val displayName = getDisplayName(context, uri)
        val cleanTitle = displayName
            ?.removeSuffix(".pdf")
            ?.removeSuffix(".PDF")
            ?.removeSuffix(".epub")
            ?.removeSuffix(".EPUB")
            ?.replace("_", " ")
            ?.replace("-", " ")
            ?.trim()
            ?: "Unknown Book"

        return when (format) {
            BookFormat.PDF -> extractPdfMetadata(context, uri, cleanTitle)
            BookFormat.EPUB -> extractEpubMetadata(context, uri, cleanTitle)
        }
    }

    private fun extractPdfMetadata(context: Context, uri: Uri, fallbackTitle: String): BookMetadata {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")!!
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            renderer.close()
            pfd.close()
            BookMetadata(
                title = fallbackTitle,
                format = BookFormat.PDF,
                totalPages = pageCount
            )
        } catch (_: Exception) {
            BookMetadata(title = fallbackTitle, format = BookFormat.PDF)
        }
    }

    private fun extractEpubMetadata(context: Context, uri: Uri, fallbackTitle: String): BookMetadata {
        var title = fallbackTitle
        var author: String? = null

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return BookMetadata(title = title, format = BookFormat.EPUB)
            val zipInputStream = java.util.zip.ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry

            while (entry != null) {
                if (entry.name.endsWith(".opf", ignoreCase = true)) {
                    val content = zipInputStream.bufferedReader().readText()

                    // Extract title from <dc:title>
                    val titleMatch = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL)
                        .find(content)
                    if (titleMatch != null) {
                        title = titleMatch.groupValues[1].trim()
                    }

                    // Extract author from <dc:creator>
                    val authorMatch = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.DOT_MATCHES_ALL)
                        .find(content)
                    if (authorMatch != null) {
                        author = authorMatch.groupValues[1].trim()
                    }
                    break
                }
                entry = zipInputStream.nextEntry
            }

            zipInputStream.close()
            inputStream.close()
        } catch (_: Exception) { }

        return BookMetadata(title = title, author = author, format = BookFormat.EPUB)
    }

    private fun getDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
