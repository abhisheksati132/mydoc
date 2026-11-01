package com.abhisheksati.mydoc.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.MessageDigest
import java.util.Locale

class PackageInstallerHelperTest {

    @Test
    fun computeCertSha256_computesCorrectLowercaseHexDigest() {
        val dummyBytes = "mydoc-test-cert-bytes".toByteArray(Charsets.UTF_8)
        val expected = MessageDigest.getInstance("SHA-256")
            .digest(dummyBytes)
            .joinToString("") { "%02x".format(it) }
            .lowercase(Locale.ROOT)

        val actual = PackageInstallerHelper.computeCertSha256(dummyBytes)
        assertEquals(expected, actual)
    }

    @Test
    fun verifyPinnedCertificate_matchesSinglePinnedFingerprint_passes() {
        val dummyBytes = "valid-cert-stream".toByteArray(Charsets.UTF_8)
        val certSha = PackageInstallerHelper.computeCertSha256(dummyBytes)

        // Single valid fingerprint with colons (common Android cert format: AA:BB:CC)
        val formattedWithColons = certSha.chunked(2).joinToString(":")

        PackageInstallerHelper.verifyPinnedCertificate(
            isDebugBuild = false,
            pinnedFingerprintsString = formattedWithColons,
            newSignatures = listOf(dummyBytes)
        )
    }

    @Test
    fun verifyPinnedCertificate_matchesOneOfMultiplePinnedFingerprints_passes() {
        val dummyBytes = "valid-cert-stream".toByteArray(Charsets.UTF_8)
        val certSha = PackageInstallerHelper.computeCertSha256(dummyBytes)

        val pinnedList = "11:22:33:44, $certSha, 55:66:77:88"

        PackageInstallerHelper.verifyPinnedCertificate(
            isDebugBuild = false,
            pinnedFingerprintsString = pinnedList,
            newSignatures = listOf(dummyBytes)
        )
    }

    @Test
    fun verifyPinnedCertificate_mismatchedFingerprintInRelease_throwsSecurityException() {
        val dummyBytes = "attacker-compromised-cert".toByteArray(Charsets.UTF_8)
        val legitimatePin = "00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff"

        val ex = assertThrows(SecurityException::class.java) {
            PackageInstallerHelper.verifyPinnedCertificate(
                isDebugBuild = false,
                pinnedFingerprintsString = legitimatePin,
                newSignatures = listOf(dummyBytes)
            )
        }
        assert(ex.message?.contains("does not match any pinned release certificate") == true)
    }

    @Test
    fun verifyPinnedCertificate_emptySignaturesList_throwsSecurityException() {
        assertThrows(SecurityException::class.java) {
            PackageInstallerHelper.verifyPinnedCertificate(
                isDebugBuild = false,
                pinnedFingerprintsString = "aabbccdd",
                newSignatures = emptyList()
            )
        }
    }

    @Test
    fun verifyPinnedCertificate_emptyPinnedStringInRelease_throwsSecurityException() {
        val dummyBytes = "cert".toByteArray()
        val ex = assertThrows(SecurityException::class.java) {
            PackageInstallerHelper.verifyPinnedCertificate(
                isDebugBuild = false,
                pinnedFingerprintsString = "   ",
                newSignatures = listOf(dummyBytes)
            )
        }
        assert(ex.message?.contains("is empty in release build") == true)
    }

    @Test
    fun verifyPinnedCertificate_debugBuild_bypassesPinCheck() {
        val dummyBytes = "any-debug-cert".toByteArray()
        // Should not throw even with totally invalid pinned string
        PackageInstallerHelper.verifyPinnedCertificate(
            isDebugBuild = true,
            pinnedFingerprintsString = "invalid_pin",
            newSignatures = listOf(dummyBytes)
        )
    }
}
