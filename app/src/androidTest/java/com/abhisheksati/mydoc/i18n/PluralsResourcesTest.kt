package com.abhisheksati.mydoc.i18n

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.abhisheksati.mydoc.R
import org.junit.Assert.assertEquals
import org.junit.Test

class PluralsResourcesTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun toolsSubtitle_pluralizes() {
        assertEquals("1 tool · 6 categories", resources.getQuantityString(R.plurals.tools_subtitle, 1, 1))
        assertEquals("5 tools · 6 categories", resources.getQuantityString(R.plurals.tools_subtitle, 5, 5))
    }

    @Test
    fun ocrCounts_pluralize() {
        assertEquals("1 word", resources.getQuantityString(R.plurals.ocr_word_count, 1, 1))
        assertEquals("3 words", resources.getQuantityString(R.plurals.ocr_word_count, 3, 3))
        assertEquals("1 character", resources.getQuantityString(R.plurals.ocr_char_count, 1, 1))
        assertEquals("42 characters", resources.getQuantityString(R.plurals.ocr_char_count, 42, 42))
    }

    @Test
    fun duplicateReviewHeader_pluralizes() {
        assertEquals(
            "1 identical copy • 2 MB each",
            resources.getQuantityString(R.plurals.cleaner_review_identical_copies, 1, 1, "2 MB")
        )
        assertEquals(
            "4 identical copies • 2 MB each",
            resources.getQuantityString(R.plurals.cleaner_review_identical_copies, 4, 4, "2 MB")
        )
    }

    @Test
    fun selectionCount_pluralizes() {
        assertEquals("1 selected", resources.getQuantityString(R.plurals.files_selected_count_plural, 1, 1))
        assertEquals("7 selected", resources.getQuantityString(R.plurals.files_selected_count_plural, 7, 7))
    }
}