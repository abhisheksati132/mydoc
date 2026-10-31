package com.abhisheksati.mydoc.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

class ApkDownloader(private val context: Context) {

    companion object {
        const val MAX_DOWNLOAD_SIZE_BYTES = 200L * 1024 * 1024 // 200 MB
        private const val MAX_REDIRECTS = 5

        private val ALLOWED_HOSTS = setOf(
            "github.com",
            "api.github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )

        fun isHostAllowed(host: String?): Boolean {
            if (host.isNullOrBlank()) return false
            val lower = host.lowercase(Locale.ROOT)
            if (lower in ALLOWED_HOSTS) return true
            if (lower == "githubusercontent.com" || lower.endsWith(".githubusercontent.com")) return true
            return false
        }

        fun validateDownloadUrl(urlString: String): URL {
            val url = URL(urlString)
            if (!url.protocol.equals("https", ignoreCase = true)) {
                throw SecurityException("Insecure protocol: Only https is allowed, got '${url.protocol}'")
            }
            if (!isHostAllowed(url.host)) {
                throw SecurityException("Disallowed host for download: '${url.host}'")
            }
            return url
        }

        fun parseChecksumContent(rawText: String): String? {
            // Checksum files usually look like "hash  filename.apk" or just "hash"
            return rawText.trim().split("\\s+".toRegex()).firstOrNull()?.takeIf { it.isNotBlank() }
        }

        fun computeSha256(file: File): String {
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

    suspend fun downloadApk(
        downloadUrl: String,
        targetFileName: String,
        expectedSha256: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanExpectedSha256 = expectedSha256.trim().lowercase(Locale.ROOT)
            if (cleanExpectedSha256.isBlank()) {
                throw IllegalArgumentException("Expected SHA-256 checksum is mandatory and cannot be empty")
            }

            val baseDir = context.cacheDir
            val updatesDir = File(baseDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            // Clean up any stale files in cache/updates before download
            updatesDir.listFiles()?.forEach { file ->
                runCatching { file.delete() }
            }

            val destinationFile = File(updatesDir, targetFileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0

            while (redirectCount <= MAX_REDIRECTS) {
                val validatedUrl = validateDownloadUrl(currentUrl)

                connection = (validatedUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "mydoc-android-updater")
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 || code == 308
                ) {
                    if (redirectCount == MAX_REDIRECTS) {
                        connection.disconnect()
                        throw SecurityException("Too many redirects: exceeded maximum of $MAX_REDIRECTS")
                    }
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

            if (totalBytes <= 0) {
                destinationFile.delete()
                throw IllegalStateException("Download rejected: Missing or invalid Content-Length header")
            }
            if (totalBytes > MAX_DOWNLOAD_SIZE_BYTES) {
                destinationFile.delete()
                throw IllegalStateException("Download rejected: Content-Length ($totalBytes bytes) exceeds 200 MB cap")
            }

            try {
                conn.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead: Long = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            totalRead += bytesRead
                            if (totalRead > MAX_DOWNLOAD_SIZE_BYTES) {
                                throw IllegalStateException("Download aborted: Stream exceeded 200 MB size limit")
                            }
                            output.write(buffer, 0, bytesRead)
                            onProgress(totalRead, totalBytes)
                        }
                        output.flush()
                    }
                }
            } catch (t: Throwable) {
                destinationFile.delete()
                throw t
            }

            // Mandatory SHA-256 verification
            val computedHash = computeSha256(destinationFile)
            if (!computedHash.equals(cleanExpectedSha256, ignoreCase = true)) {
                destinationFile.delete()
                throw IllegalStateException("Checksum mismatch! Expected $cleanExpectedSha256, got $computedHash")
            }

            destinationFile
        }
    }

    suspend fun fetchChecksumText(checksumUrl: String): String = withContext(Dispatchers.IO) {
        val validatedUrl = validateDownloadUrl(checksumUrl)
        val connection = (validatedUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "mydoc-android-updater")
        }
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            parseChecksumContent(text)
                ?: throw IllegalStateException("Empty or invalid checksum content from $checksumUrl")
        } else {
            throw IllegalStateException("Failed to fetch checksum file from $checksumUrl: HTTP ${connection.responseCode}")
        }
    }
}
