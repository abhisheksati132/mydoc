package com.abhisheksati.mydoc.explorer

import com.abhisheksati.mydoc.data.local.ExplorerZipExtraction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExplorerZipSelectionTest {
    @get:Rule val temp = TemporaryFolder()
    private fun zip(vararg entries: Pair<String, String>): File = temp.newFile("${System.nanoTime()}.zip").also { file ->
        ZipOutputStream(file.outputStream()).use { out -> entries.forEach { (name, bytes) ->
            out.putNextEntry(ZipEntry(name)); out.write(bytes.toByteArray()); out.closeEntry()
        } }
    }
    @Test fun extractsOnlySelectedFilesIntoNewFolder() = runBlocking {
        val archive = zip("docs/report.txt" to "report", "other.txt" to "other")
        val output = ExplorerZipExtraction().extract(archive, setOf("docs/report.txt"), temp.root)
        assertEquals("report", output.resolve("docs/report.txt").readText())
        assertFalse(output.resolve("other.txt").exists())
        assertFalse(temp.root.resolve("docs").exists())
    }
    @Test fun selectingFolderIncludesDescendantsAndEmptyFolders() = runBlocking {
        val archive = zip("docs/one.txt" to "one", "docs/empty/" to "", "other" to "skip")
        val output = ExplorerZipExtraction().extract(archive, setOf("docs/"), temp.root)
        assertEquals("one", output.resolve("docs/one.txt").readText())
        assertTrue(output.resolve("docs/empty").isDirectory)
        assertFalse(output.resolve("other").exists())
    }
    @Test fun previewPreservesBytesAndExtensionAndHonorsCacheLimit() = runBlocking {
        val archive = zip("docs/report.pdf" to "original bytes")
        val cache = temp.newFolder("cache")
        val preview = ExplorerZipExtraction().preview(archive, "docs/report.pdf", cache)
        assertEquals("pdf", preview.extension)
        assertEquals("original bytes", preview.readText())
        try { ExplorerZipExtraction(previewCacheBytes = 1).preview(archive, "docs/report.pdf", cache); fail("Limit ignored") }
        catch (_: java.io.IOException) { }
        assertEquals("original bytes", preview.readText())
    }
    @Test fun unsafeArchiveAndOversizedSelectionPublishNothing() = runBlocking {
        val parent = temp.newFolder("outputs")
        val bad = zip("../escape" to "bad", "safe.txt" to "safe")
        try { ExplorerZipExtraction().extract(bad, setOf("safe.txt"), parent); fail("Unsafe archive") }
        catch (_: IllegalArgumentException) { }
        val big = zip("big" to "12345")
        try { ExplorerZipExtraction(maxBytes = 2).extract(big, setOf("big"), parent); fail("Limit ignored") }
        catch (_: java.io.IOException) { }
        assertTrue(parent.listFiles()!!.isEmpty())
    }
    @Test fun repeatedExtractionsNeverReplacePreviousOutput() = runBlocking {
        val archive = zip("file" to "original")
        val first = ExplorerZipExtraction().extract(archive, setOf("file"), temp.root)
        first.resolve("file").writeText("keep")
        val second = ExplorerZipExtraction().extract(archive, setOf("file"), temp.root)
        assertNotEquals(first, second)
        assertEquals("keep", first.resolve("file").readText())
        assertEquals("original", second.resolve("file").readText())
    }
    @Test fun cancellationRemovesOnlyThisRunsStaging() = runBlocking {
        val archive = zip("file" to "bytes")
        val parent = temp.newFolder("output")
        val keep = parent.resolve("keep.txt").apply { writeText("keep") }
        try {
            ExplorerZipExtraction().extract(archive, setOf("file"), parent) { _, _ -> throw kotlinx.coroutines.CancellationException("stop") }
            fail("Cancellation swallowed")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        assertEquals(listOf("keep.txt"), parent.listFiles()!!.map { it.name })
        assertEquals("keep", keep.readText())
    }
    @Test fun changedArchiveRefusesStaleBrowserSelection() = runBlocking {
        val archive = zip("file" to "old")
        val index = com.abhisheksati.mydoc.data.local.ExplorerArchive().read(archive)
        archive.appendText("changed")
        val parent = temp.newFolder("output")
        try {
            ExplorerZipExtraction().extract(archive, setOf("file"), parent, index.stamp)
            fail("Stale selection accepted")
        } catch (_: com.abhisheksati.mydoc.data.local.ZipArchiveChanged) { }
        assertTrue(parent.listFiles()!!.isEmpty())
    }
    @Test fun checksumFailureDoesNotPublishPartialBytes() = runBlocking {
        val archive = temp.newFile("stored.zip")
        val marker = "unique payload for CRC".toByteArray()
        ZipOutputStream(archive.outputStream()).use { output ->
            val entry = ZipEntry("file.txt").apply {
                method = ZipEntry.STORED; size = marker.size.toLong(); compressedSize = size
                crc = java.util.zip.CRC32().apply { update(marker) }.value
            }
            output.putNextEntry(entry); output.write(marker); output.closeEntry()
        }
        val bytes = archive.readBytes()
        val offset = (0..bytes.size - marker.size).first { start -> marker.indices.all { bytes[start + it] == marker[it] } }
        bytes[offset] = (bytes[offset].toInt() xor 1).toByte(); archive.writeBytes(bytes)
        val parent = temp.newFolder("output")
        try { ExplorerZipExtraction().extract(archive, setOf("file.txt"), parent); fail("CRC ignored") }
        catch (_: IllegalStateException) { }
        assertTrue(parent.listFiles()!!.isEmpty())
    }

}
