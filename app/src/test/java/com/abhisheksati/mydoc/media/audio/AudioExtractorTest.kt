package com.abhisheksati.mydoc.media.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AudioExtractorTest {

    @Test
    fun testSyncFlagMapping() {
        val sampleFlags = MediaExtractor.SAMPLE_FLAG_SYNC
        val bufferFlags = AudioExtractor.mapExtractorFlagsToBufferFlags(sampleFlags)
        assertEquals(MediaCodec.BUFFER_FLAG_KEY_FRAME, bufferFlags)
    }

    @Test
    fun testPartialFrameFlagMapping() {
        val sampleFlags = MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME
        val bufferFlags = AudioExtractor.mapExtractorFlagsToBufferFlags(sampleFlags)
        assertEquals(MediaCodec.BUFFER_FLAG_PARTIAL_FRAME, bufferFlags)
        // Ensure it is NEVER mapped to BUFFER_FLAG_CODEC_CONFIG (which was the bug)
        assertNotEquals(MediaCodec.BUFFER_FLAG_CODEC_CONFIG, bufferFlags)
    }

    @Test
    fun testZeroFlagsMapping() {
        val sampleFlags = 0
        val bufferFlags = AudioExtractor.mapExtractorFlagsToBufferFlags(sampleFlags)
        assertEquals(0, bufferFlags)
    }

    @Test
    fun testCombinedFlagsMapping() {
        val sampleFlags = MediaExtractor.SAMPLE_FLAG_SYNC or MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME
        val bufferFlags = AudioExtractor.mapExtractorFlagsToBufferFlags(sampleFlags)
        val expected = MediaCodec.BUFFER_FLAG_KEY_FRAME or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        assertEquals(expected, bufferFlags)
    }
}
