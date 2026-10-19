package com.abhisheksati.mydoc.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ApkDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `computeSha256 produces exact lowercase hex digest`() {
        val testFile = tempFolder.newFile("test_payload.bin")
        val content = "Hello World! Secure updater SHA-256 test."
        testFile.writeText(content)

        val computed = ApkDownloader.computeSha256(testFile)

        // Compute reference hash using standard MessageDigest
        val expected = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray())
            .joinToString("") { "%02x".format(it) }

        assertEquals(expected, computed)
        assertEquals(64, computed.length)
    }

    @Test
    fun `computeSha256 changes on single byte tamper`() {
        val originalFile = tempFolder.newFile("original.apk")
        originalFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val originalHash = ApkDownloader.computeSha256(originalFile)

        val tamperedFile = tempFolder.newFile("tampered.apk")
        tamperedFile.writeBytes(byteArrayOf(1, 2, 3, 4, 6)) // 1 bit/byte flipped
        val tamperedHash = ApkDownloader.computeSha256(tamperedFile)

        assertNotEquals(originalHash, tamperedHash)
    }

    @Test
    fun `parseChecksumContent parses standalone hash and hash-with-filename lines`() {
        val sha = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        // Format 1: Just hash
        assertEquals(sha, ApkDownloader.parseChecksumContent(sha))

        // Format 2: sha256sum style: "hash  mydoc-v1.0.0.apk"
        assertEquals(sha, ApkDownloader.parseChecksumContent("$sha  mydoc-v1.0.0.apk\n"))

        // Format 3: leading/trailing whitespace
        assertEquals(sha, ApkDownloader.parseChecksumContent("   $sha   \t mydoc.apk\r\n"))

        // Format 4: empty or whitespace
        assertNull(ApkDownloader.parseChecksumContent("   \n\t  "))
    }
}
