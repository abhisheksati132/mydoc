package com.abhisheksati.mydoc.security.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VaultItem(
    val id: String,
    val encryptedFile: File,
    val originalName: String,
    val originalSize: Long,
    val dateAdded: Long,
    val mimeType: String
)

class VaultManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MyDocSafeVaultMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
        private const val HEADER_MAGIC = "MYDOC_VAULT_V1"
    }

    private val vaultDir: File
        get() {
            val dir = File(context.filesDir, "safe_vault")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(false) // We manage IVs explicitly
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    suspend fun getVaultItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        val files = vaultDir.listFiles { file -> file.isFile && file.name.endsWith(".vault") } ?: return@withContext emptyList()
        val items = mutableListOf<VaultItem>()

        for (file in files) {
            val header = readHeader(file) ?: continue
            items.add(
                VaultItem(
                    id = file.nameWithoutExtension,
                    encryptedFile = file,
                    originalName = header.originalName,
                    originalSize = header.originalSize,
                    dateAdded = header.dateAdded,
                    mimeType = header.mimeType
                )
            )
        }

        items.sortedByDescending { it.dateAdded }
    }

    suspend fun encryptAndMoveToVault(sourceFile: File): Result<VaultItem> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist"))
            }

            val key = getOrCreateMasterKey()
            val id = "vault_${System.currentTimeMillis()}_${SecureRandom().nextInt(10000)}"
            val targetFile = File(vaultDir, "$id.vault")

            val iv = ByteArray(IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            val originalName = sourceFile.name
            val originalSize = sourceFile.length()
            val dateAdded = System.currentTimeMillis()
            val mimeType = getMimeType(sourceFile)

            // Header metadata JSON-like binary
            val metaString = "$originalName\t$originalSize\t$dateAdded\t$mimeType"
            val metaBytes = metaString.toByteArray(StandardCharsets.UTF_8)

            FileOutputStream(targetFile).use { fos ->
                // Write Magic (14 bytes)
                fos.write(HEADER_MAGIC.toByteArray(StandardCharsets.UTF_8))
                // Write IV (12 bytes)
                fos.write(iv)
                // Write Meta Length (4 bytes)
                val metaLenBuffer = ByteBuffer.allocate(4).putInt(metaBytes.size)
                fos.write(metaLenBuffer.array())
                // Write Meta
                fos.write(metaBytes)

                // Encrypt payload
                FileInputStream(sourceFile).use { fis ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedChunk = cipher.update(buffer, 0, bytesRead)
                        if (encryptedChunk != null && encryptedChunk.isNotEmpty()) {
                            fos.write(encryptedChunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null && finalChunk.isNotEmpty()) {
                        fos.write(finalChunk)
                    }
                }
            }

            // Secure Shred source file (overwrite with zeroes then delete)
            shredAndDelete(sourceFile)

            val item = VaultItem(
                id = id,
                encryptedFile = targetFile,
                originalName = originalName,
                originalSize = originalSize,
                dateAdded = dateAdded,
                mimeType = mimeType
            )

            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decryptAndRestore(item: VaultItem, destDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()

            var targetFile = File(destDir, item.originalName)
            if (targetFile.exists()) {
                val nameWithoutExt = item.originalName.substringBeforeLast('.', item.originalName)
                val ext = item.originalName.substringAfterLast('.', "")
                val extPart = if (ext.isNotEmpty()) ".$ext" else ""
                targetFile = File(destDir, "${nameWithoutExt}_restored$extPart")
            }

            decryptFileTo(item.encryptedFile, targetFile)
            item.encryptedFile.delete()

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decryptToTemp(item: VaultItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "vault_preview")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val tempFile = File(cacheDir, item.originalName)
            decryptFileTo(item.encryptedFile, tempFile)
            Result.success(tempFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(item: VaultItem): Boolean = withContext(Dispatchers.IO) {
        try {
            shredAndDelete(item.encryptedFile)
            true
        } catch (_: Exception) {
            item.encryptedFile.delete()
        }
    }

    private fun decryptFileTo(encryptedFile: File, outputFile: File) {
        val key = getOrCreateMasterKey()
        FileInputStream(encryptedFile).use { fis ->
            // Validate Magic
            val magicBytes = ByteArray(HEADER_MAGIC.length)
            fis.read(magicBytes)
            val magic = String(magicBytes, StandardCharsets.UTF_8)
            if (magic != HEADER_MAGIC) {
                throw IllegalStateException("Invalid vault file format")
            }

            // Read IV
            val iv = ByteArray(IV_LENGTH)
            fis.read(iv)

            // Read Meta Length
            val metaLenBytes = ByteArray(4)
            fis.read(metaLenBytes)
            val metaLen = ByteBuffer.wrap(metaLenBytes).int

            // Skip Meta
            val metaBytes = ByteArray(metaLen)
            fis.read(metaBytes)

            // Init cipher for decrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val decryptedChunk = cipher.update(buffer, 0, bytesRead)
                    if (decryptedChunk != null && decryptedChunk.isNotEmpty()) {
                        fos.write(decryptedChunk)
                    }
                }
                val finalChunk = cipher.doFinal()
                if (finalChunk != null && finalChunk.isNotEmpty()) {
                    fos.write(finalChunk)
                }
            }
        }
    }

    private fun readHeader(file: File): VaultHeader? {
        return try {
            FileInputStream(file).use { fis ->
                val magicBytes = ByteArray(HEADER_MAGIC.length)
                if (fis.read(magicBytes) != HEADER_MAGIC.length) return null
                if (String(magicBytes, StandardCharsets.UTF_8) != HEADER_MAGIC) return null

                val iv = ByteArray(IV_LENGTH)
                if (fis.read(iv) != IV_LENGTH) return null

                val metaLenBytes = ByteArray(4)
                if (fis.read(metaLenBytes) != 4) return null
                val metaLen = ByteBuffer.wrap(metaLenBytes).int
                if (metaLen <= 0 || metaLen > 4096) return null

                val metaBytes = ByteArray(metaLen)
                if (fis.read(metaBytes) != metaLen) return null

                val meta = String(metaBytes, StandardCharsets.UTF_8).split("\t")
                if (meta.size < 4) return null

                VaultHeader(
                    originalName = meta[0],
                    originalSize = meta[1].toLongOrNull() ?: 0L,
                    dateAdded = meta[2].toLongOrNull() ?: System.currentTimeMillis(),
                    mimeType = meta[3]
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun shredAndDelete(file: File) {
        try {
            val length = file.length()
            if (length > 0 && length < 100L * 1024 * 1024) { // Up to 100MB
                FileOutputStream(file).use { fos ->
                    val zeroes = ByteArray(8192)
                    var written = 0L
                    while (written < length) {
                        val toWrite = minOf(zeroes.size.toLong(), length - written).toInt()
                        fos.write(zeroes, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                }
            }
        } catch (_: Exception) {}
        file.delete()
    }

    private fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: when (ext) {
                "pdf" -> "application/pdf"
                "docx", "doc" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xlsx", "xls" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "pptx", "ppt" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "mp4" -> "video/mp4"
                "mp3" -> "audio/mpeg"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
    }

    private data class VaultHeader(
        val originalName: String,
        val originalSize: Long,
        val dateAdded: Long,
        val mimeType: String
    )
}
