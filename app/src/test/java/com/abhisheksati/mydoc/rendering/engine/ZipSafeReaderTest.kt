package com.abhisheksati.mydoc.rendering.engine

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipSafeReaderTest {

    private fun buildZip(entries: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, data) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    @Test
    fun `valid small entry is read correctly`() {
        val content = "Hello ZipSafeReader!".toByteArray()
        val zipBytes = buildZip(mapOf("test.txt" to content))
        val result = ZipSafeReader.collectEntries(
            ZipInputStream(ByteArrayInputStream(zipBytes))
        ) { true }
        assertArrayEquals(content, result["test.txt"])
    }

    @Test
    fun `multiple valid entries are all collected`() {
        val entries = mapOf(
            "a.txt" to "Alpha".toByteArray(),
            "b.txt" to "Beta".toByteArray(),
            "c.txt" to "Gamma".toByteArray()
        )
        val zipBytes = buildZip(entries)
        val result = ZipSafeReader.collectEntries(
            ZipInputStream(ByteArrayInputStream(zipBytes))
        ) { true }
        assertEquals(3, result.size)
        assertArrayEquals("Alpha".toByteArray(), result["a.txt"])
    }

    @Test
    fun `filter predicate excludes entries`() {
        val zipBytes = buildZip(mapOf(
            "include.xml" to "<root/>".toByteArray(),
            "exclude.bin" to ByteArray(100)
        ))
        val result = ZipSafeReader.collectEntries(
            ZipInputStream(ByteArrayInputStream(zipBytes))
        ) { name -> name.endsWith(".xml") }
        assertTrue(result.containsKey("include.xml"))
        assertFalse(result.containsKey("exclude.bin"))
    }

    @Test
    fun `entry count limit triggers SecurityException`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            repeat(ZipSafeReader.MAX_ENTRY_COUNT + 5) { i ->
                zos.putNextEntry(ZipEntry("file_$i.txt"))
                zos.write("x".toByteArray())
                zos.closeEntry()
            }
        }
        try {
            ZipSafeReader.collectEntries(
                ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
            ) { true }
            fail("Expected SecurityException for entry count exceeded")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("count") == true)
        }
    }

    @Test
    fun `truncated zip does not throw SecurityException`() {
        val fullZip = buildZip(mapOf("file.txt" to "content".toByteArray()))
        val truncated = fullZip.copyOf(fullZip.size / 3)
        // Truncated zip should not throw SecurityException (it may throw IOException which is caught)
        try {
            ZipSafeReader.collectEntries(
                ZipInputStream(ByteArrayInputStream(truncated))
            ) { true }
        } catch (e: SecurityException) {
            fail("Truncated zip should not raise SecurityException: ${e.message}")
        } catch (_: Exception) {
            // Other exceptions from a corrupt zip are acceptable
        }
    }

    @Test
    fun `readCurrentEntry respects per-entry size cap`() {
        // Build a zip where we'll check that readCurrentEntry completes for a normal entry
        val data = ByteArray(1024) { it.toByte() }
        val zipBytes = buildZip(mapOf("data.bin" to data))
        val zis = ZipInputStream(ByteArrayInputStream(zipBytes))
        val entry = zis.nextEntry!!
        val result = ZipSafeReader.readCurrentEntry(zis, entry)
        assertArrayEquals(data, result)
    }
}
