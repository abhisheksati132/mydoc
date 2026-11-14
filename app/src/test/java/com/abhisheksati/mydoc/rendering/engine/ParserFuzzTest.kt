package com.abhisheksati.mydoc.rendering.engine

import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.XlsxReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Untrusted-input hardening: document parsers must degrade gracefully on malformed bytes.
 * A reader opens arbitrary files, so garbage must never produce a memory-safety crash
 * (NegativeArraySizeException, OutOfMemoryError, NPE, index errors) — only predictable,
 * catchable failures or an empty result.
 */
class ParserFuzzTest {

    private val random = Random(20261114L)

    private fun isSafeFailure(t: Throwable): Boolean =
        t is SecurityException ||
            t is java.io.IOException ||
            t is org.xmlpull.v1.XmlPullParserException ||
            t is IllegalArgumentException

    private suspend fun assertNoCrash(block: suspend () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            assertTrue(
                "Untrusted input caused an unsafe failure: ${t.javaClass.name}: ${t.message}",
                isSafeFailure(t)
            )
        }
    }

    private fun randomBytes(max: Int): ByteArray =
        ByteArray(random.nextInt(0, max)).also { random.nextBytes(it) }

    private fun zipWith(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, data) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun pptxParserSurvivesRandomBytes() = runBlocking {
        val parser = PptxParser()
        repeat(50) {
            val bytes = randomBytes(20_000)
            assertNoCrash { parser.parse(ByteArrayInputStream(bytes)) }
        }
    }

    @Test
    fun xlsxReaderSurvivesRandomBytes() = runBlocking {
        repeat(50) {
            val bytes = randomBytes(20_000)
            assertNoCrash { XlsxReader.read(bytes) }
        }
    }

    @Test
    fun pptxParserSurvivesGarbageXmlInValidZip() = runBlocking {
        val parser = PptxParser()
        repeat(30) {
            val bytes = zipWith(
                listOf(
                    "ppt/presentation.xml" to randomBytes(4_000),
                    "ppt/slides/slide1.xml" to randomBytes(4_000),
                    "[Content_Types].xml" to randomBytes(2_000)
                )
            )
            assertNoCrash { parser.parse(ByteArrayInputStream(bytes)) }
        }
    }

    @Test
    fun xlsxReaderSurvivesGarbageXmlInValidZip() = runBlocking {
        repeat(30) {
            val bytes = zipWith(
                listOf(
                    "xl/workbook.xml" to randomBytes(4_000),
                    "xl/sharedStrings.xml" to randomBytes(4_000),
                    "xl/worksheets/sheet1.xml" to randomBytes(4_000),
                    "xl/styles.xml" to randomBytes(2_000)
                )
            )
            assertNoCrash { XlsxReader.read(bytes) }
        }
    }

    @Test
    fun parsersSurviveTruncatedArchives() = runBlocking {
        val parser = PptxParser()
        val full = zipWith(
            listOf(
                "ppt/presentation.xml" to "<presentation/>".toByteArray(),
                "xl/workbook.xml" to "<workbook/>".toByteArray()
            )
        )
        for (cut in intArrayOf(0, 1, 4, 20, full.size / 2, full.size - 1)) {
            val truncated = full.copyOf(cut.coerceIn(0, full.size))
            assertNoCrash { parser.parse(ByteArrayInputStream(truncated)) }
            assertNoCrash { XlsxReader.read(truncated) }
        }
    }
}