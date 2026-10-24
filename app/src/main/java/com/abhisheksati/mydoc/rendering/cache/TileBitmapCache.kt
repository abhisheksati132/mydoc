package com.abhisheksati.mydoc.rendering.cache

import android.graphics.Bitmap
import android.util.LruCache
import com.abhisheksati.mydoc.domain.model.ReadingMode

class TileBitmapCache(maxMemoryBytes: Int = (Runtime.getRuntime().maxMemory() / 4).toInt()) {

    private val cache = object : LruCache<String, Bitmap>(maxMemoryBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun get(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? {
        val key = cacheKey(pageIndex, scale, mode)
        return cache.get(key)
    }

    fun put(pageIndex: Int, scale: Float, mode: ReadingMode, bitmap: Bitmap) {
        val key = cacheKey(pageIndex, scale, mode)
        cache.put(key, bitmap)
    }

    fun clear() {
        cache.evictAll()
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
