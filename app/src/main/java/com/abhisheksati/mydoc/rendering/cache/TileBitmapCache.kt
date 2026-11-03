package com.abhisheksati.mydoc.rendering.cache

import android.graphics.Bitmap
import com.abhisheksati.mydoc.domain.model.ReadingMode

/**
 * High-performance bounded LRU Bitmap Cache for PDF and document vector rendering.
 * Dynamically bounded between 16MB and 96MB to guarantee flat memory footprint across 500+ page documents.
 */
class TileBitmapCache(
    private val maxMemoryBytes: Int = (Runtime.getRuntime().maxMemory() / 4).toInt().coerceIn(16 * 1024 * 1024, 96 * 1024 * 1024)
) {

    private val lock = Any()
    private var currentBytes = 0
    private val cache = LinkedHashMap<String, Bitmap>(16, 0.75f, true)

    fun get(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? = synchronized(lock) {
        val key = cacheKey(pageIndex, scale, mode)
        val bmp = cache[key]
        if (bmp != null && runCatching { !bmp.isRecycled }.getOrDefault(true)) {
            bmp
        } else {
            if (bmp != null) {
                cache.remove(key)
                currentBytes -= runCatching { bmp.byteCount }.getOrDefault(0)
            }
            null
        }
    }

    fun put(pageIndex: Int, scale: Float, mode: ReadingMode, bitmap: Bitmap) = synchronized(lock) {
        if (runCatching { !bitmap.isRecycled }.getOrDefault(true)) {
            val key = cacheKey(pageIndex, scale, mode)
            val old = cache.put(key, bitmap)
            currentBytes += runCatching { bitmap.byteCount }.getOrDefault(0)
            if (old != null) {
                currentBytes -= runCatching { old.byteCount }.getOrDefault(0)
            }
            trimToSize(maxMemoryBytes)
        }
    }

    private fun trimToSize(maxBytes: Int) {
        val iterator = cache.entries.iterator()
        while (iterator.hasNext() && currentBytes > maxBytes) {
            val entry = iterator.next()
            iterator.remove()
            currentBytes -= runCatching { entry.value.byteCount }.getOrDefault(0)
        }
    }

    fun clear() = synchronized(lock) {
        cache.clear()
        currentBytes = 0
    }

    fun recycleAll() = synchronized(lock) {
        val bitmaps = cache.values.toList()
        cache.clear()
        currentBytes = 0
        for (bmp in bitmaps) {
            runCatching {
                if (!bmp.isRecycled) {
                    bmp.recycle()
                }
            }
        }
    }

    private fun cacheKey(pageIndex: Int, scale: Float, mode: ReadingMode): String {
        val roundedScale = (scale * 10).toInt() / 10f
        return "${pageIndex}_${roundedScale}_${mode.name}"
    }
}
