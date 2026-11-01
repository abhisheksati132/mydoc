package com.abhisheksati.mydoc.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfficeToPdfConverterTest {

    @Test
    fun generateCacheKey_deterministicForIdenticalInputs() {
        val key1 = OfficeToPdfConverter.generateCacheKey("content://com.mydoc/doc1", "report.docx")
        val key2 = OfficeToPdfConverter.generateCacheKey("content://com.mydoc/doc1", "report.docx")

        assertEquals(key1, key2)
        assertEquals(32, key1.length) // Standard 128-bit MD5 hex length
    }

    @Test
    fun generateCacheKey_distinctForDifferentUris() {
        val keyA = OfficeToPdfConverter.generateCacheKey("content://com.mydoc/doc1", "report.docx")
        val keyB = OfficeToPdfConverter.generateCacheKey("content://com.mydoc/doc2", "report.docx")

        assertNotEquals(keyA, keyB)
    }

    @Test
    fun generateCacheKey_distinctForDifferentFileNames() {
        val keyA = OfficeToPdfConverter.generateCacheKey("content://com.mydoc/doc1", "report.docx")
        val keyB = OfficeToPdfConverter.generateCacheKey("content://com.mydoc/doc1", "report_v2.docx")

        assertNotEquals(keyA, keyB)
    }

    @Test
    fun generateCacheKey_handlesSpecialCharactersAndSpacesSafely() {
        val key = OfficeToPdfConverter.generateCacheKey(
            "content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2F",
            "Financial Plan (2026) - Final & Approved #1.xlsx"
        )

        assertTrue(key.isNotEmpty())
        assertEquals(32, key.length)
    }

    @Test
    fun cacheFileManagement_measuresSizesAndDeletesCleanly() {
        val tempDir = File.createTempFile("cache_test", "dir").apply {
            delete()
            mkdirs()
        }
        val cacheSubDir = File(tempDir, "office_pdf_cache").apply { mkdirs() }

        try {
            val fileA = File(cacheSubDir, "docA.pdf").apply { writeBytes(ByteArray(1024)) }
            val fileB = File(cacheSubDir, "docB.pdf").apply { writeBytes(ByteArray(2048)) }

            val totalBytes = cacheSubDir.listFiles()?.sumOf { it.length() } ?: 0L
            assertEquals(3072L, totalBytes)

            // Simulate clearing cache
            var bytesFreed = 0L
            cacheSubDir.listFiles()?.forEach {
                bytesFreed += it.length()
                it.delete()
            }
            assertEquals(3072L, bytesFreed)
            assertEquals(0L, cacheSubDir.listFiles()?.sumOf { it.length() } ?: 0L)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
