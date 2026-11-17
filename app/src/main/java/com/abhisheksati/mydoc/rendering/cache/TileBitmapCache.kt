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
    private val cache = ByteBoundedLru<Bitmap> { runCatching { it.byteCount }.getOrDefault(0) }

    init {
        register(this)
    }

    companion object {
        private val activeCaches = java.util.Collections.newSetFromMap(java.util.WeakHashMap<TileBitmapCache, Boolean>())

        fun register(cache: TileBitmapCache) {
            synchronized(activeCaches) {
                activeCaches.add(cache)
            }
        }

        fun trimAll(level: Int) {
            val snapshot = synchronized(activeCaches) { activeCaches.toList() }
            for (cache in snapshot) {
                cache.onTrimMemory(level)
            }
        }

        fun clearAll() {
            val snapshot = synchronized(activeCaches) { activeCaches.toList() }
            for (cache in snapshot) {
                cache.clear()
            }
        }
    }

    fun get(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? = synchronized(lock) {
        val key = cacheKey(pageIndex, scale, mode)
        val bmp = cache[key]
        if (bmp != null && runCatching { !bmp.isRecycled }.getOrDefault(true)) {
            bmp
        } else {
            if (bmp != null) {
                cache.remove(key)
            }
            null
        }
    }

    fun put(pageIndex: Int, scale: Float, mode: ReadingMode, bitmap: Bitmap) = synchronized(lock) {
        if (runCatching { !bitmap.isRecycled }.getOrDefault(true)) {
            cache.put(cacheKey(pageIndex, scale, mode), bitmap)
            cache.trimTo(maxMemoryBytes)
        }
    }

    fun clear() = synchronized(lock) {
        cache.clear()
    }

    fun onTrimMemory(level: Int) = synchronized(lock) {
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
                level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                clear()
            }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                cache.trimTo(maxMemoryBytes / 2)
            }
        }
    }

    fun recycleAll() = synchronized(lock) {
        cache.clear()
    }

    private fun cacheKey(pageIndex: Int, scale: Float, mode: ReadingMode): String {
        val roundedScale = (scale * 10).toInt() / 10f
        return "${pageIndex}_${roundedScale}_${mode.name}"
    }
}
