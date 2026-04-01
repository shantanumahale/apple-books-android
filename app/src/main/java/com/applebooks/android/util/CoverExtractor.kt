package com.applebooks.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object CoverExtractor {

    fun extractPdfCover(context: Context, uri: Uri, bookId: Long): String? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) {
                renderer.close()
                pfd.close()
                return null
            }

            val page = renderer.openPage(0)
            val width = 300
            val height = (width.toFloat() / page.width * page.height).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()

            saveCoverBitmap(context, bitmap, bookId)
        } catch (_: Exception) {
            null
        }
    }

    fun extractEpubCover(context: Context, uri: Uri, bookId: Long): String? {
        return try {
            // Simple EPUB cover extraction: look for cover image in the ZIP
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val zipInputStream = java.util.zip.ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            var coverBytes: ByteArray? = null

            while (entry != null) {
                val name = entry.name.lowercase()
                if (!entry.isDirectory && (name.contains("cover") || name.contains("title")) &&
                    (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))
                ) {
                    coverBytes = zipInputStream.readBytes()
                    break
                }
                entry = zipInputStream.nextEntry
            }

            // If no cover image found by name, look for first image
            if (coverBytes == null) {
                inputStream.close()
                val retryStream = context.contentResolver.openInputStream(uri) ?: return null
                val retryZip = java.util.zip.ZipInputStream(retryStream)
                entry = retryZip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory &&
                        (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) &&
                        !name.contains("icon") && !name.contains("logo")
                    ) {
                        coverBytes = retryZip.readBytes()
                        break
                    }
                    entry = retryZip.nextEntry
                }
                retryZip.close()
                retryStream.close()
            } else {
                zipInputStream.close()
                inputStream.close()
            }

            if (coverBytes != null) {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
                if (bitmap != null) {
                    // Scale down if too large
                    val scaled = if (bitmap.width > 300) {
                        val ratio = 300f / bitmap.width
                        Bitmap.createScaledBitmap(bitmap, 300, (bitmap.height * ratio).toInt(), true)
                    } else {
                        bitmap
                    }
                    saveCoverBitmap(context, scaled, bookId)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun saveCoverBitmap(context: Context, bitmap: Bitmap, bookId: Long): String {
        val coversDir = File(context.filesDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        val file = File(coversDir, "$bookId.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    fun deleteCover(path: String) {
        try {
            File(path).delete()
        } catch (_: Exception) { }
    }
}
