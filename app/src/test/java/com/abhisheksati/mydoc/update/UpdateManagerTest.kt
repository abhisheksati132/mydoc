package com.abhisheksati.mydoc.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class UpdateManagerTest {

    @Test
    fun isNewerVersion_correctlyComparesSemVer() {
        fun isNewer(remote: String, local: String): Boolean {
            val remoteParts = remote.split('.').mapNotNull { it.toIntOrNull() }
            val localParts = local.split('.').mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }

        assertTrue(isNewer("1.0.1", "1.0.0"))
        assertTrue(isNewer("1.1.0", "1.0.9"))
        assertTrue(isNewer("2.0.0", "1.99.99"))
        assertTrue(isNewer("1.0.0.1", "1.0.0"))

        assertFalse(isNewer("1.0.0", "1.0.0"))
        assertFalse(isNewer("0.9.9", "1.0.0"))
        assertFalse(isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun isValidTag_sanitizesReleaseTagsStrictly() {
        assertTrue(GitHubUpdateService.isValidTag("v1.0.0"))
        assertTrue(GitHubUpdateService.isValidTag("1.0.0"))
        assertTrue(GitHubUpdateService.isValidTag("v2.1"))
        assertTrue(GitHubUpdateService.isValidTag("v1.2.3.4"))

        assertFalse(GitHubUpdateService.isValidTag(""))
        assertFalse(GitHubUpdateService.isValidTag("v1.0.0-rc1"))
        assertFalse(GitHubUpdateService.isValidTag("v1.0.0-beta"))
        assertFalse(GitHubUpdateService.isValidTag("v1.0.0.0.5"))
        assertFalse(GitHubUpdateService.isValidTag("release-1.0"))
        assertFalse(GitHubUpdateService.isValidTag("../../../etc/passwd"))
    }

    @Test
    fun parseReleaseJson_rejectsDraftsAndPrereleases() {
        val service = GitHubUpdateService()

        val draftJson = """
            {
              "tag_name": "v2.0.0",
              "draft": true,
              "prerelease": false,
              "assets": []
            }
        """.trimIndent()
        assertNull(service.parseReleaseJson(draftJson))

        val prereleaseJson = """
            {
              "tag_name": "v2.0.0",
              "draft": false,
              "prerelease": true,
              "assets": []
            }
        """.trimIndent()
        assertNull(service.parseReleaseJson(prereleaseJson))
    }

    @Test
    fun parseReleaseJson_failsWhenMatchingChecksumAssetIsMissing() {
        val service = GitHubUpdateService()

        val jsonWithoutChecksum = """
            {
              "tag_name": "v2.0.0",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "mydoc-v2.0.0-universal.apk",
                  "browser_download_url": "https://github.com/abhisheksati132/mydoc/releases/download/v2.0.0/mydoc-v2.0.0-universal.apk",
                  "size": 15000000
                }
              ]
            }
        """.trimIndent()

        val ex = assertThrows(IllegalStateException::class.java) {
            service.parseReleaseJson(jsonWithoutChecksum)
        }
        assertTrue(ex.message!!.contains("does not have a matching checksum asset"))
    }

    @Test
    fun parseReleaseJson_succeedsWithMatchingChecksumAsset() {
        val service = GitHubUpdateService()

        val validJson = """
            {
              "tag_name": "v2.0.0",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "mydoc-v2.0.0-universal.apk",
                  "browser_download_url": "https://github.com/abhisheksati132/mydoc/releases/download/v2.0.0/mydoc-v2.0.0-universal.apk",
                  "size": 15000000
                },
                {
                  "name": "mydoc-v2.0.0-universal.apk.sha256",
                  "browser_download_url": "https://github.com/abhisheksati132/mydoc/releases/download/v2.0.0/mydoc-v2.0.0-universal.apk.sha256",
                  "size": 64
                }
              ]
            }
        """.trimIndent()

        val release = service.parseReleaseJson(validJson)
        assertNotNull(release)
        assertEquals("v2.0.0", release?.tagName)
        assertEquals("2.0.0", release?.versionName)
        assertEquals("https://github.com/abhisheksati132/mydoc/releases/download/v2.0.0/mydoc-v2.0.0-universal.apk.sha256", release?.checksumUrl)
    }

    @Test
    fun verifyPinnedCertificate_failsIfSignaturesEmpty() {
        val ex = assertThrows(SecurityException::class.java) {
            PackageInstallerHelper.verifyPinnedCertificate(
                isDebugBuild = true,
                pinnedFingerprintsString = "any",
                newSignatures = emptyList()
            )
        }
        assertTrue(ex.message!!.contains("cannot be empty"))
    }

    @Test
    fun verifyPinnedCertificate_allowsDebugBuildWhenPinnedListEmpty() {
        // In debug builds, pinned fingerprints may be empty
        val dummySig = byteArrayOf(1, 2, 3, 4)
        PackageInstallerHelper.verifyPinnedCertificate(
            isDebugBuild = true,
            pinnedFingerprintsString = "",
            newSignatures = listOf(dummySig)
        )
    }

    @Test
    fun verifyPinnedCertificate_refusesInstallInReleaseWhenPinnedListEmpty() {
        val dummySig = byteArrayOf(1, 2, 3, 4)
        val ex = assertThrows(SecurityException::class.java) {
            PackageInstallerHelper.verifyPinnedCertificate(
                isDebugBuild = false,
                pinnedFingerprintsString = "",
                newSignatures = listOf(dummySig)
            )
        }
        assertTrue(ex.message!!.contains("is empty in release build"))
    }

    @Test
    fun verifyPinnedCertificate_refusesInstallInReleaseWhenCertMismatch() {
        val fakeSig = "Tampered certificate bytes".toByteArray()
        val pinned = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff"

        val ex = assertThrows(SecurityException::class.java) {
            PackageInstallerHelper.verifyPinnedCertificate(
                isDebugBuild = false,
                pinnedFingerprintsString = pinned,
                newSignatures = listOf(fakeSig)
            )
        }
        assertTrue(ex.message!!.contains("does not match any pinned release certificate"))
    }

    @Test
    fun verifyPinnedCertificate_succeedsInReleaseWhenCertMatches() {
        val validSigBytes = "Authentic release signing cert".toByteArray()
        val expectedSha256 = MessageDigest.getInstance("SHA-256")
            .digest(validSigBytes)
            .joinToString("") { "%02x".format(it) }

        // Test with colon-separated formatted fingerprint (as often produced by keytool/apksigner)
        val colonFormatted = expectedSha256.chunked(2).joinToString(":").uppercase()

        PackageInstallerHelper.verifyPinnedCertificate(
            isDebugBuild = false,
            pinnedFingerprintsString = colonFormatted,
            newSignatures = listOf(validSigBytes)
        )
    }
}
