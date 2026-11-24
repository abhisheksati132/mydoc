package com.abhisheksati.mydoc.tools.ui.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class ImagePdfStagingTest {
    @get:Rule val temp = TemporaryFolder()

    private fun target() = File(temp.root, "image.jpg")

    @Test fun successfulImportPreservesBytes() = runBlocking {
        val target = target()
        val bytes = byteArrayOf(1, 2, 3)
        assertEquals(target, stageEditorImage(target, maxBytes = 8) { ByteArrayInputStream(bytes) })
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun oversizedImportRemovesPartialFile() = runBlocking {
        val target = target()
        assertNull(stageEditorImage(target, maxBytes = 8500) { ByteArrayInputStream(ByteArray(9000)) })
        assertFalse(target.exists())
    }

    @Test fun inputExactlyAtLimitSucceeds() = runBlocking {
        val target = target()
        val bytes = ByteArray(8) { it.toByte() }
        assertEquals(target, stageEditorImage(target, maxBytes = 8) { ByteArrayInputStream(bytes) })
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun failedReadRemovesPartialFileAndClosesStream() = runBlocking {
        val target = target()
        var reads = 0
        var closed = false
        val input = object : InputStream() {
            override fun read(): Int = error("bulk reads expected")
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (reads++ > 0) throw IOException("provider disconnected")
                b[off] = 1
                return 1
            }
            override fun close() { closed = true }
        }
        assertNull(stageEditorImage(target, maxBytes = 8) { input })
        assertFalse(target.exists())
        assertTrue(closed)
    }

    @Test fun emptyOrUnavailableInputDoesNotLeaveFile() = runBlocking {
        val target = target()
        assertNull(stageEditorImage(target) { null })
        assertFalse(target.exists())
        assertNull(stageEditorImage(target) { ByteArrayInputStream(byteArrayOf()) })
        assertFalse(target.exists())
    }

    @Test fun cancellationPropagatesAndRemovesPartialFile() {
        val target = target()
        assertThrows(CancellationException::class.java) {
            runBlocking {
                val job = currentCoroutineContext().job
                val input = object : InputStream() {
                    var reads = 0
                    override fun read(): Int = error("bulk reads expected")
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (reads++ > 0) return -1
                        b[off] = 1
                        job.cancel()
                        return 1
                    }
                }
                stageEditorImage(target) { input }
            }
        }
        assertFalse(target.exists())
    }
}
