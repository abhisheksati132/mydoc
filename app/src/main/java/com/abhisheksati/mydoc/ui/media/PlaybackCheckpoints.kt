package com.abhisheksati.mydoc.ui.media

import kotlin.math.abs

/**
 * When a playing item is worth writing to the database, and what value to write.
 *
 * Both players used to save only from `onDispose`, through a scope bound to the composition that is
 * being cancelled at that moment — so the write was usually dropped and "resume where you left off"
 * rarely worked. Saving is now a checkpoint while playing, plus a flush when playback stops and when
 * the app is backgrounded, none of which depend on a dying scope. The cost is honest and stated:
 * leaving the player can lose up to one checkpoint interval of position.
 */
object PlaybackCheckpoints {

    /** Below this there is nothing worth resuming. */
    const val MIN_POSITION_MS = 1_000L

    /** How far the position has to move before another write is justified. */
    const val CHECKPOINT_INTERVAL_MS = 5_000L

    /** Within this of the end the item is considered finished, so the next open starts at zero. */
    const val NEAR_END_MS = 3_000L

    /**
     * The value to persist, or null when there is nothing to persist. An unknown duration and a
     * position still inside the first second are both "not yet"; a position in the last few seconds
     * is a completed item and resets rather than resuming to a point the viewer has already seen.
     */
    fun valueToSave(positionMs: Long, durationMs: Long): Long? = when {
        durationMs <= 0L -> null
        positionMs < MIN_POSITION_MS -> null
        positionMs >= durationMs - NEAR_END_MS -> 0L
        else -> positionMs
    }

    /** True when [positionMs] is far enough from the last checkpoint to be worth writing. */
    fun shouldPersist(lastCheckpointMs: Long, positionMs: Long): Boolean =
        abs(positionMs - lastCheckpointMs) >= CHECKPOINT_INTERVAL_MS

    /**
     * A stop or a move to the background writes whatever the viewer actually reached, subject to the
     * same "is there anything to resume" rule. [lastSavedValue] is what was written last, so a stop
     * one poll after a checkpoint does not repeat it.
     */
    fun shouldFlush(lastSavedValue: Long?, positionMs: Long, durationMs: Long): Boolean {
        val value = valueToSave(positionMs, durationMs) ?: return false
        return value != lastSavedValue
    }
}

/**
 * Applies [PlaybackCheckpoints] to one media item and remembers what was already written, so the
 * callers in the two players are the same size and this rule can be tested without a player.
 *
 * [save] is injected rather than taken from a repository so a test can count the writes and see the
 * values, which is the whole point of the finding.
 */
class PlaybackCheckpointWriter(
    private val save: suspend (value: Long, durationMs: Long) -> Unit
) {
    var lastWrittenValue: Long?
        private set
    var lastWrittenPosition: Long = -1L
        private set

    init {
        lastWrittenValue = null
    }

    /** Record that the reader was put at [positionMs] by a resume, so it is not written back. */
    fun seed(positionMs: Long) {
        lastWrittenPosition = positionMs
        lastWrittenValue = positionMs
    }

    /**
     * Returns the value written, or null when nothing was worth writing. [force] is a stop or a move
     * to the background: it ignores the cadence gate but still refuses to repeat the same value.
     */
    suspend fun checkpoint(positionMs: Long, durationMs: Long, force: Boolean = false): Long? {
        val value = PlaybackCheckpoints.valueToSave(positionMs, durationMs) ?: return null
        val worthACadenceWrite = !force && PlaybackCheckpoints.shouldPersist(lastWrittenPosition, positionMs)
        val worthAFlush = force && value != lastWrittenValue
        if (!worthACadenceWrite && !worthAFlush) return null
        save(value, durationMs)
        lastWrittenValue = value
        lastWrittenPosition = positionMs
        return value
    }
}
