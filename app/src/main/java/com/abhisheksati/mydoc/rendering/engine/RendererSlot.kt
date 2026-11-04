package com.abhisheksati.mydoc.rendering.engine

/**
 * Holds the one document engine a viewer session is allowed to own.
 *
 * MuPDF documents are native allocations that the garbage collector does not reclaim, and the
 * viewer used to keep one per document URI: a reader who opened fifteen files held fifteen native
 * documents until the Activity's store was cleared. Replacing through this slot closes what it is
 * displacing, and releasing empties it, so the count of live engines is bounded by one regardless
 * of how many documents pass through.
 */
class RendererSlot {

    private var held: DocumentRenderer? = null

    /** Number of engines currently owned — one, or none. */
    val liveCount: Int
        get() = if (held == null) 0 else 1

    /** The owned engine, if any. */
    val current: DocumentRenderer?
        get() = held

    /**
     * Takes ownership of [next] and releases whatever was held. [next] is returned so callers can
     * keep using it; when it is null the slot ends up empty and the previous engine is closed.
     */
    fun replace(next: DocumentRenderer?): DocumentRenderer? {
        val previous = held
        if (previous === next) return next
        held = next
        previous?.closeQuietly()
        return next
    }

    /** Gives up and closes the owned engine. Safe to call more than once. */
    fun release() {
        val previous = held
        held = null
        previous?.closeQuietly()
    }

    private fun DocumentRenderer.closeQuietly() {
        runCatching { close() }
    }
}
