package com.abhisheksati.mydoc.ui.cleaner

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CleanerFormatSizeLocaleTest {

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
    fun megabytesUseDefaultLocaleDigits() {
        Locale.setDefault(Locale("ar", "EG"))
        val formatted = formatSize(1_572_864)
        assertTrue("expected Arabic-Indic digits, got: $formatted", formatted.startsWith("\u0661"))
        assertTrue(formatted.contains("MB"))
        assertFalse("Locale.US formatting leaked into the cleaner sheet", formatted.contains("1.5"))
    }

    @Test
    fun byteCountsUseDefaultLocaleDigits() {
        Locale.setDefault(Locale("ar", "EG"))
        val formatted = formatSize(500)
        assertTrue("expected Arabic-Indic digits, got: $formatted", formatted.contains("\u0665\u0660\u0660"))
    }

    @Test
    fun usLocaleFormattingUnchanged() {
        Locale.setDefault(Locale.US)
        assertEquals("1.5 MB", formatSize(1_572_864))
        assertEquals("500 B", formatSize(500))
        assertEquals("0 B", formatSize(0))
        assertEquals("1.0 GB", formatSize(1_073_741_824))
    }
}