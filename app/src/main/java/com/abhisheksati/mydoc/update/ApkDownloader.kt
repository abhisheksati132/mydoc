package com.abhisheksati.mydoc.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ApkDownloader(private val context: Context) {

    suspend fun downloadApk(
        downloadUrl: String,
        targetFileName: String,
        expectedSha256: String? = null,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(updatesDir, targetFileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0

            // Follow HTTP redirects safely (GitHub releases redirect to AWS S3/CDN)
            while (redirectCount < 5) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "mydoc-android-updater")
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirected with no Location header")
                    currentUrl = location
                    connection.disconnect()
                    redirectCount++
                } else if (code == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    throw IllegalStateException("Failed to download APK: HTTP $code")
                }
            }

            val conn = connection ?: throw IllegalStateException("Connection could not be established")
            val totalBytes = conn.contentLengthLong

            conn.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead, totalBytes)
                    }
                    output.flush()
                }
            }

            // Verify SHA-256 checksum if provided
            if (!expectedSha256.isNullOrBlank()) {
                val computedHash = computeSha256(destinationFile)
                val expectedClean = expectedSha256.trim().lowercase()
                if (!computedHash.equals(expectedClean, ignoreCase = true)) {
                    destinationFile.delete()
                    throw IllegalStateException("Checksum mismatch! Expected $expectedClean, got $computedHash")
                }
            }

            destinationFile
        }
    }

    suspend fun fetchChecksumText(checksumUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(checksumUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "mydoc-android-updater")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                // Checksum files usually look like "hash  filename.apk" or just "hash"
                text.trim().split("\\s+".toRegex()).firstOrNull()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
