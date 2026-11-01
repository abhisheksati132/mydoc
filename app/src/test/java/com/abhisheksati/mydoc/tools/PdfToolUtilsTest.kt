package com.abhisheksati.mydoc.tools

import com.abhisheksati.mydoc.tools.engine.PdfToolUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfToolUtilsTest {

    @Test
    fun parsePageRanges_blankOrEmpty_returnsAllPages() {
        assertEquals(listOf(0, 1, 2, 3, 4), PdfToolUtils.parsePageRanges("", 5))
        assertEquals(listOf(0, 1, 2, 3, 4), PdfToolUtils.parsePageRanges("   ", 5))
        assertEquals(emptyList<Int>(), PdfToolUtils.parsePageRanges("", 0))
    }

    @Test
    fun parsePageRanges_singlePages_parsesCorrectly() {
        assertEquals(listOf(0), PdfToolUtils.parsePageRanges("1", 5))
        assertEquals(listOf(4), PdfToolUtils.parsePageRanges("5", 5))
        assertEquals(listOf(0, 2, 4), PdfToolUtils.parsePageRanges("1, 3, 5", 5))
    }

    @Test
    fun parsePageRanges_contiguousRange_parsesRange() {
        assertEquals(listOf(1, 2, 3), PdfToolUtils.parsePageRanges("2-4", 5))
        assertEquals(listOf(0, 1, 2, 3, 4), PdfToolUtils.parsePageRanges("1-5", 5))
    }

    @Test
    fun parsePageRanges_invertedRange_normalizesGracefully() {
        // Human error: "5-2" should normalize to 2 through 5
        assertEquals(listOf(1, 2, 3, 4), PdfToolUtils.parsePageRanges("5-2", 5))
    }

    @Test
    fun parsePageRanges_openBounds_handlesDefaults() {
        // "-3" should mean start from 1 up to 3
        assertEquals(listOf(0, 1, 2), PdfToolUtils.parsePageRanges("-3", 5))
        // "3-" should mean from 3 to end
        assertEquals(listOf(2, 3, 4), PdfToolUtils.parsePageRanges("3-", 5))
    }

    @Test
    fun parsePageRanges_overlappingAndDuplicates_deduplicatesAndSorts() {
        assertEquals(listOf(0, 1, 2, 3), PdfToolUtils.parsePageRanges("1-3, 2-4, 2, 3", 5))
    }

    @Test
    fun parsePageRanges_outOfBounds_clampsOrIgnoresSafely() {
        // Page 4-99 in a 5-page document includes page 4 (idx 3) and page 5 (idx 4)
        assertEquals(listOf(3, 4), PdfToolUtils.parsePageRanges("4-99", 5))
        // Page 5-99 in a 5-page document includes only page 5 (idx 4)
        assertEquals(listOf(4), PdfToolUtils.parsePageRanges("5-99", 5))
        assertEquals(emptyList<Int>(), PdfToolUtils.parsePageRanges("10-20", 5))
    }

    @Test
    fun parsePageRanges_malformedInput_ignoresGarbage() {
        assertEquals(listOf(1), PdfToolUtils.parsePageRanges("abc, - , , 2, xyz", 5))
        assertEquals(emptyList<Int>(), PdfToolUtils.parsePageRanges("invalid", 5))
    }

    @Test
    fun sanitizeXml_escapesSpecialEntities() {
        val input = """<document title="Report & Analysis" author='O'Connor'>"""
        val sanitized = PdfToolUtils.sanitizeXml(input)
        assertEquals(
            "&lt;document title=&quot;Report &amp; Analysis&quot; author=&apos;O&apos;Connor&apos;&gt;",
            sanitized
        )
    }

    @Test
    fun sanitizeXml_stripsInvalidControlCharacters() {
        // Control character \u0000 (null byte) and \u0007 (bell) are illegal in XML
        val input = "Hello\u0000World\u0007!\nTab:\t"
        val sanitized = PdfToolUtils.sanitizeXml(input)
        assertEquals("HelloWorld!\nTab:\t", sanitized)
    }

    @Test
    fun createTempOutputFile_createsExpectedDirectoryAndFile() {
        val tempDir = java.io.File.createTempFile("cache_test", "dir").apply {
            delete()
            mkdirs()
        }
        try {
            val file = PdfToolUtils.createTempOutputFile(tempDir, "sample", "pdf")
            org.junit.Assert.assertTrue(file.parentFile?.exists() == true)
            org.junit.Assert.assertEquals("tools_output", file.parentFile?.name)
            org.junit.Assert.assertTrue(file.name.startsWith("mydoc_sample_"))
            org.junit.Assert.assertTrue(file.name.endsWith(".pdf"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
