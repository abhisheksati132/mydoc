package com.abhisheksati.mydoc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentFileTest {

    @Test
    fun extensionExtraction_isCorrect() {
        val file1 = createTestFile("resume.pdf")
        assertEquals("pdf", file1.extension)
        assertEquals("PDF", file1.typeBadge)
        assertEquals(DocumentCategory.PDF, file1.category)

        val file2 = createTestFile("financial_report.2024.final.XLSX")
        assertEquals("xlsx", file2.extension)
        assertEquals("EXCEL", file2.typeBadge)
        assertEquals(DocumentCategory.EXCEL, file2.category)

        val file3 = createTestFile("notes.txt")
        assertEquals("txt", file3.extension)
        assertEquals("TXT", file3.typeBadge)
        assertEquals(DocumentCategory.TEXT, file3.category)

        val file4 = createTestFile("presentation.PPTX")
        assertEquals("pptx", file4.extension)
        assertEquals("PPT", file4.typeBadge)
        assertEquals(DocumentCategory.PPT, file4.category)

        val file5 = createTestFile("proposal.docx")
        assertEquals("docx", file5.extension)
        assertEquals("WORD", file5.typeBadge)
        assertEquals(DocumentCategory.WORD, file5.category)

        val file6 = createTestFile("data.csv")
        assertEquals("csv", file6.extension)
        assertEquals("CSV", file6.typeBadge)
        assertEquals(DocumentCategory.EXCEL, file6.category)

        val file7 = createTestFile("script.py")
        assertEquals("py", file7.extension)
        assertEquals(DocumentCategory.TEXT, file7.category)

        val file8 = createTestFile("main.kt")
        assertEquals("kt", file8.extension)
        assertEquals(DocumentCategory.TEXT, file8.category)

        val file9 = createTestFile("config.json")
        assertEquals("json", file9.extension)
        assertEquals(DocumentCategory.TEXT, file9.category)

        val file10 = createTestFile("README.md")
        assertEquals("md", file10.extension)
        assertEquals(DocumentCategory.TEXT, file10.category)
    }

    @Test
    fun formattedSize_isCorrect() {
        val zeroBytes = createTestFile("empty.txt", sizeBytes = 0)
        assertEquals("Unknown size", zeroBytes.formattedSize)

        val bytes = createTestFile("tiny.txt", sizeBytes = 500)
        assertEquals("500 B", bytes.formattedSize)

        val kilobytes = createTestFile("doc.pdf", sizeBytes = 2048)
        assertEquals("2.0 KB", kilobytes.formattedSize)

        val megabytes = createTestFile("slides.pptx", sizeBytes = 5 * 1024 * 1024 + 512 * 1024)
        assertEquals("5.5 MB", megabytes.formattedSize)

        val gigabytes = createTestFile("archive.pdf", sizeBytes = 2L * 1024 * 1024 * 1024)
        assertEquals("2.0 GB", gigabytes.formattedSize)
    }

    private fun createTestFile(displayName: String, sizeBytes: Long = 1024): DocumentFile {
        return DocumentFile(
            uri = "content://com.android.providers.media.documents/document/123",
            displayName = displayName,
            sizeBytes = sizeBytes,
            mimeType = "application/octet-stream",
            lastOpenedTimestamp = System.currentTimeMillis()
        )
    }
}
