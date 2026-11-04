package com.abhisheksati.mydoc.tools.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Whether a saved signature is usable, not merely whether bytes exist on disk. */
enum class SignatureStatus {
    /** Nothing was ever saved. */
    NONE,

    /** Saved and decryptable; signing works with it as it stands. */
    READY,

    /**
     * Something is stored but MyDoc cannot open it - the archive is corrupt, or the Keystore key
     * behind it is gone. Redrawing is the only way forward, and "no signature found" would send
     * the reader looking for a file that is actually sitting there.
     */
    UNREADABLE
}

/**
 * Secure on-device storage for user signatures.
 *
 * Encrypts saved signature image bytes with hardware-backed Android Keystore AES-256-GCM.
 * Decryption never creates a key: an absent alias means the old ciphertext can never be opened
 * again, and silently minting a replacement used to turn that into a mystery "no signature
 * found" while the file stayed on disk.
 */
object SignatureStorage {

    private const val TAG = "SignatureStorage"
    private const val KEY_ALIAS = "MyDoc_Signature_Key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val SIGNATURE_FILENAME = "user_signature.enc"

    private fun signatureFile(context: Context) = File(context.filesDir, SIGNATURE_FILENAME)

    private fun existingKey(): SecretKey? = runCatching {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }.getOrNull()

    private fun createKey(): SecretKey {
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
     * Encrypts and writes the signature. The archive is staged and renamed into place so an
     * interruption cannot leave the previous signature half-overwritten; the old code wrote
     * straight to the live file, and a crash there destroyed it.
     */
    fun saveSignature(context: Context, bitmap: Bitmap): Boolean {
        val targetFile = signatureFile(context)
        val staging = File(targetFile.parentFile, "$SIGNATURE_FILENAME.tmp")
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val plainBytes = stream.toByteArray()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: createKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainBytes)

            FileOutputStream(staging).use { fos ->
                fos.write(iv)
                fos.write(cipherBytes)
                fos.flush()
            }
            if (!staging.renameTo(targetFile)) {
                staging.copyTo(targetFile, overwrite = true)
                staging.delete()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Signature could not be saved", e)
            staging.delete()
            false
        }
    }

    fun status(context: Context): SignatureStatus = when (decode(signatureFile(context))) {
        is DecodeResult.Ok -> SignatureStatus.READY
        is DecodeResult.Failure -> SignatureStatus.UNREADABLE
        DecodeResult.Absent -> SignatureStatus.NONE
    }

    /** Null means there is nothing usable. Call [status] to find out why, and act differently. */
    fun loadSignature(context: Context): Bitmap? =
        (decode(signatureFile(context)) as? DecodeResult.Ok)?.bitmap

    /** What to tell the reader when signing cannot go ahead, which differs by cause. */
    fun problemMessage(context: Context): String? = when (status(context)) {
        SignatureStatus.NONE -> "No signature found. Please draw or import your signature first."
        SignatureStatus.UNREADABLE -> "The saved signature can\'t be opened on this device. Draw or import it again."
        SignatureStatus.READY -> null
    }

    fun clearSignature(context: Context): Boolean {
        val targetFile = signatureFile(context)
        return !targetFile.exists() || targetFile.delete()
    }

    /** Only a signature that can actually be opened counts as saved. */
    fun hasSavedSignature(context: Context): Boolean = status(context) == SignatureStatus.READY

    private sealed class DecodeResult {
        data object Absent : DecodeResult()
        data class Ok(val bitmap: Bitmap) : DecodeResult()
        data class Failure(val reason: String) : DecodeResult()
    }

    private fun decode(file: File): DecodeResult {
        if (!file.exists() || file.length() <= GCM_IV_LENGTH) return DecodeResult.Absent
        val key = existingKey()
            ?: return DecodeResult.Failure("the device key that encrypted it is gone")
        return try {
            val allBytes = FileInputStream(file).use { it.readBytes() }
            val iv = allBytes.copyOfRange(0, GCM_IV_LENGTH)
            val cipherBytes = allBytes.copyOfRange(GCM_IV_LENGTH, allBytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plainBytes = cipher.doFinal(cipherBytes)
            val bitmap = BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size)
            if (bitmap == null) DecodeResult.Failure("the decrypted bytes are not an image")
            else DecodeResult.Ok(bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "Stored signature could not be decrypted", e)
            DecodeResult.Failure("the stored signature is corrupt")
        }
    }
}
