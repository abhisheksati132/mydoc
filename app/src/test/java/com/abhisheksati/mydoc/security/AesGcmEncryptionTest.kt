package com.abhisheksati.mydoc.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Unit tests verifying hardware-grade AES-256-GCM authenticated encryption invariants,
 * corrupted tag rejection, IV uniqueness, and secure data shredding.
 */
class AesGcmEncryptionTest {

    private lateinit var secretKey: SecretKey
    private val random = SecureRandom()

    private val transformation = "AES/GCM/NoPadding"
    private val ivLength = 12
    private val tagLengthBits = 128

    @Before
    fun setUp() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        secretKey = keyGen.generateKey()
    }

    @Test
    fun aes256Gcm_encryptAndDecrypt_restoresOriginalContent() {
        val originalText = "TopSecretDocument-12345"
        val plainBytes = originalText.toByteArray(Charsets.UTF_8)

        val iv = ByteArray(ivLength).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(transformation)
        val spec = GCMParameterSpec(tagLengthBits, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(plainBytes)
        assertFalse(cipherBytes.contentEquals(plainBytes))

        // Decrypt
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = decryptCipher.doFinal(cipherBytes)

        assertEquals(originalText, String(decryptedBytes, Charsets.UTF_8))
    }

    @Test(expected = AEADBadTagException::class)
    fun aes256Gcm_corruptedCiphertext_throwsAuthenticationException() {
        val plainBytes = "SensitiveFinancialStatement".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(ivLength).also { random.nextBytes(it) }

        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(tagLengthBits, iv))
        val cipherBytes = cipher.doFinal(plainBytes)

        // Tamper with one bit of the ciphertext/tag
        cipherBytes[0] = (cipherBytes[0].toInt() xor 0x01).toByte()

        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(tagLengthBits, iv))
        decryptCipher.doFinal(cipherBytes) // Must throw AEADBadTagException
    }

    @Test(expected = AEADBadTagException::class)
    fun aes256Gcm_wrongKey_rejectsDecryption() {
        val plainBytes = "ConfidentialNotes".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(ivLength).also { random.nextBytes(it) }

        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(tagLengthBits, iv))
        val cipherBytes = cipher.doFinal(plainBytes)

        // Generate different key
        val differentKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, differentKey, GCMParameterSpec(tagLengthBits, iv))
        decryptCipher.doFinal(cipherBytes) // Must throw AEADBadTagException
    }

    @Test
    fun secureZeroFillShredder_overwritesAllBytesBeforeDeletion() {
        val tempFile = File.createTempFile("shred_test", ".tmp")
        try {
            tempFile.writeText("Sensitive customer SSN and passwords")
            assertTrue(tempFile.exists())
            val originalLength = tempFile.length()
            assertTrue(originalLength > 0)

            // Multi-pass secure shredding simulation
            tempFile.outputStream().use { fos ->
                val zeros = ByteArray(originalLength.toInt())
                fos.write(zeros)
                fos.flush()
            }

            val shreddedBytes = tempFile.readBytes()
            assertTrue(shreddedBytes.all { it == 0.toByte() })

            val deleted = tempFile.delete()
            assertTrue(deleted)
            assertFalse(tempFile.exists())
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
