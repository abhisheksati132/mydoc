package com.abhisheksati.mydoc.domain.model

import android.net.Uri
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaItemLocaleTest {
    private fun item(durationMs: Long = 0L) = MediaItem(
        id = 1L,
        uri = Uri.parse("content://media/external/images/media/1"),
        displayName = "clip.mp4",
        sizeBytes = 1_572_864L,
        mimeType = "video/mp4",
        dateAddedSec = 1700000000L,
        durationMs = durationMs
    )

    @Test
    fun formattedSize_followsDefaultLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,5 MB", item().formattedSize)
            Locale.setDefault(Locale.US)
            assertEquals("1.5 MB", item().formattedSize)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun formattedDuration_isStableAcrossLocales() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("01:05", item(durationMs = 65_000L).formattedDuration)
            assertEquals("1:01:05", item(durationMs = 3_665_000L).formattedDuration)
        } finally {
            Locale.setDefault(previous)
        }
    }
}