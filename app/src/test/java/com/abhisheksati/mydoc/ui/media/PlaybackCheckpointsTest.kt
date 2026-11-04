package com.abhisheksati.mydoc.ui.media

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N-3: "resume where you left off" was launched from the scope being cancelled as the screen left
 * composition, so it usually never reached Room. Saving is now a checkpoint while playing plus a
 * flush on stop and on backgrounding. These tests drive the same sequences the finding lists and
 * count the writes that actually happen.
 */
class PlaybackCheckpointsTest {

    private class Recorder {
        val writes = mutableListOf<Pair<Long, Long>>()
        suspend fun save(value: Long, durationMs: Long) { writes += value to durationMs }
        val lastValue: Long? get() = writes.lastOrNull()?.first
    }

    private fun writer(recorder: Recorder) = PlaybackCheckpointWriter(recorder::save)

    @Test
    fun nothingIsWorthSavingInsideTheFirstSecond() {
        assertNull(PlaybackCheckpoints.valueToSave(0L, 60_000L))
        assertNull(PlaybackCheckpoints.valueToSave(999L, 60_000L))
        assertNull("an unknown duration is not a licence to save garbage", PlaybackCheckpoints.valueToSave(30_000L, 0L))
    }

    @Test
    fun aFinishedItemResumesFromTheStart() {
        assertEquals(0L, PlaybackCheckpoints.valueToSave(59_000L, 60_000L))
        assertEquals(0L, PlaybackCheckpoints.valueToSave(57_001L, 60_000L))
        assertEquals(56_000L, PlaybackCheckpoints.valueToSave(56_000L, 60_000L))
    }

    @Test
    fun aNormalSessionCheckpointsAboutEveryFiveSecondsAndNotEveryFrame() = runBlocking {
        val recorder = Recorder()
        val subject = writer(recorder)
        // 250 ms polling for two minutes.
        var position = 0L
        while (position <= 120_000L) {
            subject.checkpoint(position, 120_000L)
            position += 250L
        }
        // 480 polls, one write every five seconds of playback: the database is not touched per frame.
        assertEquals(24, recorder.writes.size)
        assertEquals(5_000L, recorder.writes.first().first)
        assertEquals("playing to the end resets rather than resuming past the last scene", 0L, recorder.writes.last().first)
        assertEquals(listOf(5_000L, 10_000L, 15_000L), recorder.writes.take(3).map { it.first })
    }

    @Test
    fun leavingThePlayerNormallyFlushesThePositionActuallyReached() = runBlocking {
        val recorder = Recorder()
        val subject = writer(recorder)
        subject.checkpoint(1_000L, 120_000L)
        subject.checkpoint(1_250L, 120_000L)
        assertEquals("small moves must not write", 0, recorder.writes.size)

        // The screen is going away: the poll loop ends and asks for a flush.
        val written = subject.checkpoint(2_400L, 120_000L, force = true)
        assertEquals(2_400L, written)
        assertEquals("only the stop flush reached the database", 1, recorder.writes.size)
        assertEquals(2_400L, recorder.lastValue)
    }

    @Test
    fun backgroundingWritesOnceAndDoesNotRepeatTheSameValue() = runBlocking {
        val recorder = Recorder()
        val subject = writer(recorder)
        subject.checkpoint(40_000L, 300_000L)
        subject.checkpoint(41_000L, 300_000L, force = true)
        subject.checkpoint(41_000L, 300_000L, force = true)
        subject.checkpoint(41_500L, 300_000L, force = true)
        assertEquals(listOf(40_000L, 41_000L, 41_500L), recorder.writes.map { it.first })
    }

    @Test
    fun aResumedItemDoesNotImmediatelyRewriteWhatItJustRead() = runBlocking {
        val recorder = Recorder()
        val subject = writer(recorder)
        subject.seed(90_000L)
        // A resumed position is not written back to itself, but a stop that moved a little is.
        assertNull(subject.checkpoint(90_250L, 300_000L))
        assertEquals(90_250L, subject.checkpoint(90_250L, 300_000L, force = true))
        assertEquals(0, recorder.writes.size - 1)
        assertEquals(96_000L, subject.checkpoint(96_000L, 300_000L))
    }

    @Test
    fun reopeningTheSameMediaAfterATruncatedSessionResumesWhereItStopped() = runBlocking {
        val recorder = Recorder()
        val first = writer(recorder)
        var position = 0L
        while (position <= 63_000L) {
            first.checkpoint(position, 120_000L)
            position += 250L
        }
        first.checkpoint(63_000L, 120_000L, force = true)
        val stored = recorder.lastValue
        assertEquals(63_000L, stored)

        // Second open: the app reads the stored value back and seeds the new writer with it.
        val resumed = PlaybackCheckpoints.valueToSave(stored!!, 120_000L)
        assertEquals(63_000L, resumed)
        val second = writer(recorder)
        second.seed(resumed!!)
        val writesBeforeThisOpen = recorder.writes.size
        assertNull(second.checkpoint(resumed, 120_000L))
        assertEquals("reopening wrote the position back unchanged", writesBeforeThisOpen, recorder.writes.size)
    }

    @Test
    fun anItemPlayedToEndIsResetRatherThanSavedAtTheEnd() = runBlocking {
        val recorder = Recorder()
        val subject = writer(recorder)
        subject.checkpoint(100_000L, 120_000L)
        subject.checkpoint(119_000L, 120_000L, force = true)
        assertEquals(listOf(100_000L, 0L), recorder.writes.map { it.first })
    }

    @Test
    fun scrubbingBackwardsAlsoProducesAReadableCheckpoint() = runBlocking {
        val recorder = Recorder()
        val subject = writer(recorder)
        subject.checkpoint(100_000L, 300_000L)
        val afterSeek = subject.checkpoint(20_000L, 300_000L, force = true)
        assertEquals(20_000L, afterSeek)
    }

    @Test
    fun aWriterThatCannotSaveStillReportsNothingWritten() = runBlocking {
        var calls = 0
        val subject = PlaybackCheckpointWriter { _, _ -> calls++ }
        assertNull(subject.checkpoint(500L, 60_000L))
        assertEquals(0, calls)
    }
}
