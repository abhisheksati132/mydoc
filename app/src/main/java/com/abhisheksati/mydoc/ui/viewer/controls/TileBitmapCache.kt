package com.abhisheksati.mydoc.ui.viewer.controls

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.LruCache
import com.abhisheksati.mydoc.domain.model.ReadingMode

/**
 * High-performance LRU cache for high-scale regional zoom tiles.
 * Capped at 24MB to ensure flat memory usage across endless zooming.
 */
object TileBitmapCache {
    private const val MAX_CACHE_BYTES = 24 * 1024 * 1024 // 24 MB

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (oldValue !== newValue && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    fun makeKey(docUri: String, pageIndex: Int, region: RectF, scale: Float, mode: ReadingMode): String {
        val l = (region.left * 100).toInt()
        val t = (region.top * 100).toInt()
        val r = (region.right * 100).toInt()
        val b = (region.bottom * 100).toInt()
        val s = (scale * 10).toInt()
        return "${docUri}_${pageIndex}_${l}_${t}_${r}_${b}_${s}_${mode.name}"
    }

    fun get(key: String): Bitmap? = synchronized(cache) {
        val bmp = cache.get(key)
        if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun put(key: String, bitmap: Bitmap) = synchronized(cache) {
        if (!bitmap.isRecycled) {
            cache.put(key, bitmap)
        }
    }

    fun clear() = synchronized(cache) {
        cache.evictAll()
    }
}
