package com.abhisheksati.mydoc.security

import com.abhisheksati.mydoc.tools.security.SignatureStorage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Verifies the AES-256-GCM archive invariants of the production signature store:
 * [SignatureStorage.sealWithKey] and [SignatureStorage.openWithKey] are the exact helpers
 * `saveSignature`/`decode` run against the Android Keystore key, exercised here with a
 * JCA-generated key because the Keystore itself only exists on a device.
 */
class AesGcmEncryptionTest {

    private lateinit var secretKey: SecretKey
    private val random = SecureRandom()

    private val ivLength = 12
    private val gcmTagLength = 16

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        secretKey = keyGen.generateKey()
    }

    @Test
    fun sealWithKey_openWithKey_restoresOriginalContent() {
        val plainBytes = "TopSecretDocument-12345".toByteArray(Charsets.UTF_8)

        val archive = SignatureStorage.sealWithKey(secretKey, plainBytes)

        assertFalse(archive.contentEquals(plainBytes))
        assertEquals(plainBytes.size + ivLength + gcmTagLength, archive.size)
        assertArrayEquals(plainBytes, SignatureStorage.openWithKey(secretKey, archive))
    }

    @Test
    fun sealWithKey_usesAFreshIvForEveryArchive() {
        val plainBytes = "SameContent".toByteArray(Charsets.UTF_8)

        val first = SignatureStorage.sealWithKey(secretKey, plainBytes)
        val second = SignatureStorage.sealWithKey(secretKey, plainBytes)

        assertFalse(
            "Two seals of the same plaintext must not reuse an IV",
            first.copyOfRange(0, ivLength).contentEquals(second.copyOfRange(0, ivLength))
        )
        assertNotEquals(first.toList(), second.toList())
        assertArrayEquals(plainBytes, SignatureStorage.openWithKey(secretKey, first))
        assertArrayEquals(plainBytes, SignatureStorage.openWithKey(secretKey, second))
    }

    @Test
    fun corruptedCiphertext_throwsAuthenticationException() {
        val plainBytes = "SensitiveFinancialStatement".toByteArray(Charsets.UTF_8)
        val archive = SignatureStorage.sealWithKey(secretKey, plainBytes)

        archive[ivLength] = (archive[ivLength].toInt() xor 0x01).toByte()

        assertThrows(AEADBadTagException::class.java) {
            SignatureStorage.openWithKey(secretKey, archive)
        }
    }

    @Test
    fun corruptedTag_throwsAuthenticationException() {
        val plainBytes = "TamperTheTag".toByteArray(Charsets.UTF_8)
        val archive = SignatureStorage.sealWithKey(secretKey, plainBytes)

        archive[archive.lastIndex] = (archive[archive.lastIndex].toInt() xor 0x01).toByte()

        assertThrows(AEADBadTagException::class.java) {
            SignatureStorage.openWithKey(secretKey, archive)
        }
    }

    @Test
    fun wrongKey_rejectsDecryption() {
        val plainBytes = "ConfidentialNotes".toByteArray(Charsets.UTF_8)
        val archive = SignatureStorage.sealWithKey(secretKey, plainBytes)
        val differentKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        assertThrows(AEADBadTagException::class.java) {
            SignatureStorage.openWithKey(differentKey, archive)
        }
    }

    @Test
    fun archiveWithoutIvOrCiphertext_isRejected() {
        val tooShort = ByteArray(ivLength).also { random.nextBytes(it) }

        assertThrows(IllegalArgumentException::class.java) {
            SignatureStorage.openWithKey(secretKey, tooShort)
        }
    }
}
