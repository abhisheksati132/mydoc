package com.abhisheksati.mydoc.tools.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure on-device storage for user signatures.
 * Encrypts saved signature image bytes using hardware-backed Android Keystore AES-256-GCM.
 */
object SignatureStorage {

    private const val KEY_ALIAS = "MyDoc_Signature_Key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val SIGNATURE_FILENAME = "user_signature.enc"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val keyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (keyEntry != null) {
                return keyEntry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts and writes signature bitmap to app's private files directory.
     */
    fun saveSignature(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val plainBytes = stream.toByteArray()

            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainBytes)

            val targetFile = File(context.filesDir, SIGNATURE_FILENAME)
            FileOutputStream(targetFile).use { fos ->
                fos.write(iv)
                fos.write(cipherBytes)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads and decrypts stored signature bitmap.
     */
    fun loadSignature(context: Context): Bitmap? {
        val targetFile = File(context.filesDir, SIGNATURE_FILENAME)
        if (!targetFile.exists() || targetFile.length() <= GCM_IV_LENGTH) return null

        return try {
            val allBytes = FileInputStream(targetFile).use { it.readBytes() }
            val iv = allBytes.copyOfRange(0, GCM_IV_LENGTH)
            val cipherBytes = allBytes.copyOfRange(GCM_IV_LENGTH, allBytes.size)

            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherBytes)

            BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if an encrypted signature exists.
     */
    fun hasSavedSignature(context: Context): Boolean {
        val targetFile = File(context.filesDir, SIGNATURE_FILENAME)
        return targetFile.exists() && targetFile.length() > GCM_IV_LENGTH
    }

    /**
     * Deletes saved encrypted signature.
     */
    fun clearSignature(context: Context): Boolean {
        val targetFile = File(context.filesDir, SIGNATURE_FILENAME)
        return if (targetFile.exists()) targetFile.delete() else true
    }
}
