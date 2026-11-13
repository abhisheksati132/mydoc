package com.abhisheksati.mydoc.ui.media

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MediaTimeFormatLocaleTest {

    private lateinit var original: Locale

    @Before
    fun setUp() {
        original = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(original)
    }

    @Test
    fun audioTimeUsesDefaultLocaleDigits() {
        Locale.setDefault(Locale("ar", "EG"))
        assertEquals("\u0660\u0661:\u0660\u0665", formatAudioTime(65_000))
    }

    @Test
    fun videoTimeUsesDefaultLocaleDigits() {
        Locale.setDefault(Locale("ar", "EG"))
        assertEquals("\u0660\u0661:\u0660\u0665", formatTime(65_000))
        assertEquals("\u0661:\u0660\u0661:\u0660\u0661", formatTime(3_661_000))
    }

    @Test
    fun audioTimeStillCorrectInUsLocale() {
        Locale.setDefault(Locale.US)
        assertEquals("01:05", formatAudioTime(65_000))
        assertEquals("00:00", formatAudioTime(-5))
    }

    @Test
    fun videoTimeStillCorrectInUsLocale() {
        Locale.setDefault(Locale.US)
        assertEquals("01:05", formatTime(65_000))
        assertEquals("1:01:01", formatTime(3_661_000))
        assertEquals("00:00", formatTime(0))
    }
}