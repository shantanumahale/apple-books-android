package com.applebooks.android.reader.pdf

import android.graphics.Bitmap
import android.util.LruCache

class PdfBitmapCache(maxPages: Int = 7) {

    private val cache = object : LruCache<Int, Bitmap>(maxPages) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Int,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    @Synchronized
    fun get(page: Int): Bitmap? {
        val bitmap = cache.get(page)
        if (bitmap != null && bitmap.isRecycled) {
            cache.remove(page)
            return null
        }
        return bitmap
    }

    @Synchronized
    fun put(page: Int, bitmap: Bitmap) {
        val existing = cache.get(page)
        if (existing != null && !existing.isRecycled) {
            existing.recycle()
        }
        cache.put(page, bitmap)
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }
}
