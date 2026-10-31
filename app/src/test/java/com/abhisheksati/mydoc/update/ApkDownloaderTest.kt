package com.abhisheksati.mydoc.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun `validateDownloadUrl accepts valid https GitHub hosts`() {
        val validUrls = listOf(
            "https://github.com/abhisheksati132/mydoc/releases/download/v1.0.0/app-universal.apk",
            "https://api.github.com/repos/abhisheksati132/mydoc/releases/assets/12345",
            "https://objects.githubusercontent.com/github-production-release-asset-2e65be/123/456",
            "https://release-assets.githubusercontent.com/download/app.apk"
        )

        for (urlString in validUrls) {
            val url = ApkDownloader.validateDownloadUrl(urlString)
            assertNotNull(url)
            assertEquals("https", url.protocol)
        }
    }

    @Test
    fun `validateDownloadUrl rejects insecure http protocol`() {
        val httpUrl = "http://github.com/abhisheksati132/mydoc/releases/download/v1.0.0/app.apk"
        val ex = assertThrows(SecurityException::class.java) {
            ApkDownloader.validateDownloadUrl(httpUrl)
        }
        assertTrue(ex.message!!.contains("Insecure protocol", ignoreCase = true))
    }

    @Test
    fun `validateDownloadUrl rejects disallowed external hosts (redirect attack)`() {
        val rogueUrls = listOf(
            "https://malicious-server.com/fake-mydoc.apk",
            "https://github.evil.com/app.apk",
            "https://notgithubusercontent.com/asset.apk",
            "https://api.github.com.attacker.com/payload.apk"
        )

        for (urlString in rogueUrls) {
            val ex = assertThrows(SecurityException::class.java) {
                ApkDownloader.validateDownloadUrl(urlString)
            }
            assertTrue(ex.message!!.contains("Disallowed host", ignoreCase = true))
        }
    }

    @Test
    fun `isHostAllowed correctly filters domains`() {
        assertTrue(ApkDownloader.isHostAllowed("github.com"))
        assertTrue(ApkDownloader.isHostAllowed("api.github.com"))
        assertTrue(ApkDownloader.isHostAllowed("objects.githubusercontent.com"))
        assertTrue(ApkDownloader.isHostAllowed("release-assets.githubusercontent.com"))
        assertTrue(ApkDownloader.isHostAllowed("any-subdomain.githubusercontent.com"))

        assertFalse(ApkDownloader.isHostAllowed(null))
        assertFalse(ApkDownloader.isHostAllowed(""))
        assertFalse(ApkDownloader.isHostAllowed("fakegithub.com"))
        assertFalse(ApkDownloader.isHostAllowed("github.com.hacker.com"))
    }

    @Test
    fun `max download size is capped at 200MB`() {
        assertEquals(200L * 1024 * 1024, ApkDownloader.MAX_DOWNLOAD_SIZE_BYTES)
    }
}
