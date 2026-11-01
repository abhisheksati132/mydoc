package com.abhisheksati.mydoc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentFilterTest {

    @Test
    fun allMatchesEverything() {
        assertTrue(DocumentFilter.ALL.matches("pdf"))
        assertTrue(DocumentFilter.ALL.matches("mp4"))
        assertTrue(DocumentFilter.ALL.matches(""))
    }

    @Test
    fun pdfMatchesOnlyPdf() {
        assertTrue(DocumentFilter.PDF.matches("pdf"))
        assertTrue(DocumentFilter.PDF.matches("PDF"))
        assertFalse(DocumentFilter.PDF.matches("docx"))
        assertFalse(DocumentFilter.PDF.matches("epub"))
    }

    @Test
    fun officeFamiliesMatchTheirExtensions() {
        listOf("doc", "docx", "dot", "dotx", "odt", "rtf").forEach {
            assertTrue("Word should match $it", DocumentFilter.WORD.matches(it))
        }
        listOf("xls", "xlsx", "xlsm", "csv", "ods").forEach {
            assertTrue("Excel should match $it", DocumentFilter.EXCEL.matches(it))
        }
        listOf("ppt", "pptx", "pps", "odp").forEach {
            assertTrue("PPT should match $it", DocumentFilter.PPT.matches(it))
        }
        listOf("txt", "log", "md", "json", "xml").forEach {
            assertTrue("Text should match $it", DocumentFilter.TEXT.matches(it))
        }
    }

    @Test
    fun familiesDoNotOverlap() {
        assertFalse(DocumentFilter.WORD.matches("xlsx"))
        assertFalse(DocumentFilter.EXCEL.matches("pptx"))
        assertFalse(DocumentFilter.PPT.matches("txt"))
        assertFalse(DocumentFilter.TEXT.matches("pdf"))
    }

    @Test
    fun chipsExposeEveryFormatButAll() {
        assertEquals(
            listOf("PDF", "Word", "Excel", "PPT", "Text"),
            DocumentFilter.formats.map { it.label }
        )
    }
}
