package com.abhisheksati.mydoc.rendering.cache

/**
 * Size-bounded LRU map. TileBitmapCache delegates its byte accounting here so the bound and
 * eviction order can be exercised without constructing an android.graphics.Bitmap, which a
 * JVM unit test cannot do.
 */
internal class ByteBoundedLru<V>(private val weigh: (V) -> Int) {

    private val entries = LinkedHashMap<String, V>(16, 0.75f, true)

    var currentBytes: Int = 0
        private set

    val size: Int get() = entries.size

    operator fun get(key: String): V? = entries[key]

    fun put(key: String, value: V) {
        val previous = entries.put(key, value)
        currentBytes += weigh(value)
        if (previous != null) currentBytes -= weigh(previous)
    }

    fun remove(key: String): V? = entries.remove(key)?.also { currentBytes -= weigh(it) }

    fun clear() {
        entries.clear()
        currentBytes = 0
    }

    fun trimTo(maxBytes: Int) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext() && currentBytes > maxBytes) {
            val entry = iterator.next()
            iterator.remove()
            currentBytes -= weigh(entry.value)
        }
    }
}
