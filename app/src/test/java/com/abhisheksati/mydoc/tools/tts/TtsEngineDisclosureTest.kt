package com.abhisheksati.mydoc.tools.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsEngineDisclosureTest {

    @Test
    fun nullDefaultEngineYieldsNoName() {
        assertNull(ttsEngineDisplayName(null) { "Label" })
    }

    @Test
    fun blankDefaultEngineYieldsNoName() {
        assertNull(ttsEngineDisplayName("  ") { "Label" })
    }

    @Test
    fun readableLabelIsPreferred() {
        assertEquals("Speech Services", ttsEngineDisplayName("com.google.android.tts") { "Speech Services" })
    }

    @Test
    fun missingLabelFallsBackToPackageName() {
        assertEquals("com.google.android.tts", ttsEngineDisplayName("com.google.android.tts") { null })
        assertEquals("com.google.android.tts", ttsEngineDisplayName("com.google.android.tts") { "  " })
    }

    @Test
    fun throwingLabelLoaderFallsBackToPackageName() {
        assertEquals(
            "com.example.engine",
            ttsEngineDisplayName("com.example.engine") { throw IllegalStateException("package gone") }
        )
    }
}