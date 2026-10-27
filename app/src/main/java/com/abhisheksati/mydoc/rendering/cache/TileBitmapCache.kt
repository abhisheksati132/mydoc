package com.abhisheksati.mydoc.rendering.cache

import android.graphics.Bitmap
import android.util.LruCache
import com.abhisheksati.mydoc.domain.model.ReadingMode

/**
 * High-performance bounded LRU Bitmap Cache for PDF and document vector rendering.
 * Dynamically bounded between 16MB and 96MB to guarantee flat memory footprint across 500+ page documents.
 */
class TileBitmapCache(
    maxMemoryBytes: Int = (Runtime.getRuntime().maxMemory() / 4).toInt().coerceIn(16 * 1024 * 1024, 96 * 1024 * 1024)
) {

    private val cache = object : LruCache<String, Bitmap>(maxMemoryBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun get(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val key = cacheKey(pageIndex, scale, mode)
        val bmp = cache.get(key)
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun put(pageIndex: Int, scale: Float, mode: ReadingMode, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            val key = cacheKey(pageIndex, scale, mode)
            cache.put(key, bitmap)
        }
    }

    fun clear() {
        cache.evictAll()
    }

    fun evictFarPages(visibleRange: IntRange) {
        val snapshot = cache.snapshot()
        for ((key, bmp) in snapshot) {
            val pageIdx = key.substringBefore('_').toIntOrNull() ?: continue
            if (pageIdx !in (visibleRange.first - 3)..(visibleRange.last + 3)) {
                cache.remove(key)
                try {
                    if (bmp != null && !bmp.isRecycled) {
                        bmp.recycle()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun recycleAll() {
        val snapshot = cache.snapshot()
        cache.evictAll()
        for ((_, bmp) in snapshot) {
            try {
                if (bmp != null && !bmp.isRecycled) {
                    bmp.recycle()
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun cacheKey(pageIndex: Int, scale: Float, mode: ReadingMode): String {
        val roundedScale = (scale * 10).toInt() / 10f
        return "${pageIndex}_${roundedScale}_${mode.name}"
    }
}
