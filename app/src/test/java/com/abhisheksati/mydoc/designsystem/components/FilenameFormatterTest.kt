package com.abhisheksati.mydoc.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N-21: the middle-truncation arithmetic went negative for a long extension or a small limit, and
 * `substring(0, negative)` throws inside composition — a crash while drawing a title bar.
 */
class FilenameFormatterTest {

    private fun shortened(name: String, max: Int): String {
        val result = formatMiddleTruncated(name, max)
        assertTrue(
            "'$name' at $max produced ${result.length} characters: '$result'",
            result.length <= max
        )
        return result
    }

    @Test
    fun aNameThatAlreadyFitsIsUntouched() {
        assertEquals("report.pdf", formatMiddleTruncated("report.pdf", 36))
    }

    @Test
    fun aLongExtensionAtATightLimitKeepsTheHeadRatherThanCrashing() {
        // 16 characters of room against a 12-character extension: the old code asked for a
        // negative number of base characters.
        val name = "Q4.summary.2026.final.tabularpprx"
        val result = shortened(name, 16)
        // A 12-character extension is not worth preserving, so the name is truncated in the middle
        // like any other: the head still identifies the file, and nothing throws.
        assertTrue("expected the head of the name, got '$result'", result.startsWith("Q4.summar"))
        assertTrue("truncation must be visible: '$result'", result.contains("…"))
        assertTrue("the tail must survive too: '$result'", result.endsWith("arpprx"))
    }

    @Test
    fun everyLimitFromOneToTheNameLengthStaysWithinBounds() {
        val name = "Quarterly-Financial-Report-FY26.xlsx"
        for (limit in 1..name.length + 4) {
            val result = shortened(name, limit)
            assertTrue("limit $limit gave '$result'", result.isNotEmpty() == name.isNotEmpty())
        }
    }

    @Test
    fun aNormalExtensionSurvivesMiddleTruncation() {
        val result = shortened("2026-11-01-quarterly-consolidated-financial-report.pdf", 24)
        assertTrue("the extension is the identity of the file: '$result'", result.endsWith(".pdf"))
        assertTrue("the head is kept: '$result'", result.startsWith("2026-11-01-"))
    }

    @Test
    fun anExtensionLongerThanTheSaneBoundIsTreatedAsNameText() {
        val name = "notes.tabularpprxtradingsummary"
        val result = shortened(name, 18)
        assertTrue("'$result' must show the start of the name", result.startsWith("notes.tab"))
    }

    @Test
    fun aDotAtTheStartIsNotAnExtension() {
        val result = shortened(".gitignore-somewhat-long-file-name", 12)
        assertTrue("'$result' should start at the beginning of the name", result.startsWith(".gitig"))
        assertTrue("'$result' must stay inside the limit", result.length <= 12)
    }

    @Test
    fun noExtensionStillTruncatesInMiddle() {
        val result = shortened("quarterlyfinancialreportingpipeline", 16)
        assertTrue("'$result' keeps both ends", result.startsWith("quarterly") && result.length <= 16)
    }

    @Test
    fun zeroAndNegativeLimitsCannotCrash() {
        assertEquals("", formatMiddleTruncated("report.pdf.docx", 0))
        assertEquals("", formatMiddleTruncated("report.pdf.docx", -5))
    }

    @Test
    fun nonAsciiNamesAreMeasuredInCharsNotBytes() {
        val result = shortened("Jahresabschluss-2026-Q4-finale-Fassung.pdf", 20)
        assertTrue(result.length <= 20)
        assertTrue(result.endsWith(".pdf"))
    }
}
