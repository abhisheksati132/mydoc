package com.abhisheksati.mydoc.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

class ApkDownloader(private val context: Context, private val cacheFolder: String = "updates") {

    companion object {
        // The universal APK includes all offline Word engine ABIs (~267 MiB in 1.9.6).
        const val MAX_DOWNLOAD_SIZE_BYTES = 384L * 1024 * 1024
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
            return rawText.trim().split("\\s+".toRegex(), limit = 2).firstOrNull()
                ?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
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
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
        accept: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanExpectedSha256 = expectedSha256.trim().lowercase(Locale.ROOT)
            if (cleanExpectedSha256.isBlank()) {
                throw IllegalArgumentException("Expected SHA-256 checksum is mandatory and cannot be empty")
            }

            val baseDir = context.cacheDir
            require(cacheFolder in setOf("updates", "office-engine-download"))
            val updatesDir = File(baseDir, cacheFolder).apply {
                if (!exists()) mkdirs()
            }
            require(targetFileName == File(targetFileName).name && targetFileName.endsWith(".apk"))
            val destinationFile = File(updatesDir, targetFileName)
            val stagedFile = File.createTempFile("download-", ".part", updatesDir)

            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0

            try {
                while (redirectCount <= MAX_REDIRECTS) {
                    val validatedUrl = validateDownloadUrl(currentUrl)

                    connection = (validatedUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "mydoc-android-updater")
                        setRequestProperty("Accept-Encoding", "identity")
                        if (accept != null) setRequestProperty("Accept", accept)
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
                    stagedFile.delete()
                    throw IllegalStateException("Download rejected: Missing or invalid Content-Length header")
                }
                if (totalBytes > MAX_DOWNLOAD_SIZE_BYTES) {
                    stagedFile.delete()
                    throw IllegalStateException("Download rejected: Content-Length ($totalBytes bytes) exceeds the $MAX_DOWNLOAD_SIZE_BYTES byte cap")
                }

                try {
                    conn.inputStream.use { input ->
                        FileOutputStream(stagedFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead: Long = 0
                            var reportedAt = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                totalRead += bytesRead
                                if (totalRead > MAX_DOWNLOAD_SIZE_BYTES) {
                                    throw IllegalStateException("Download aborted: Stream exceeded the $MAX_DOWNLOAD_SIZE_BYTES byte size limit")
                                }
                                output.write(buffer, 0, bytesRead)
                                val now = System.nanoTime()
                                if (now - reportedAt >= 100_000_000L) {
                                    onProgress(totalRead, totalBytes)
                                    reportedAt = now
                                }
                            }
                            output.flush()
                            check(totalRead == totalBytes) { "The APK download ended before all bytes arrived." }
                            onProgress(totalRead, totalBytes)
                        }
                    }
                } catch (t: Throwable) {
                    stagedFile.delete()
                    throw t
                }

                // Mandatory SHA-256 verification
                val computedHash = computeSha256(stagedFile)
                if (!computedHash.equals(cleanExpectedSha256, ignoreCase = true)) {
                    stagedFile.delete()
                    throw IllegalStateException("Checksum mismatch! Expected $cleanExpectedSha256, got $computedHash")
                }

                check(stagedFile.renameTo(destinationFile)) { "Could not publish verified APK" }
                destinationFile
            } finally {
                connection?.disconnect()
                stagedFile.delete()
            }
        }
    }

    suspend fun fetchChecksumText(checksumUrl: String): String = withContext(Dispatchers.IO) {
        var url = validateDownloadUrl(checksumUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "mydoc-android-updater")
            }
            try {
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    check(redirectCount < MAX_REDIRECTS) { "Too many checksum redirects" }
                    val location = checkNotNull(connection.getHeaderField("Location"))
                    url = validateDownloadUrl(URL(url, location).toString())
                    return@repeat
                }
                check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                    "Failed to fetch checksum file: HTTP ${connection.responseCode}"
                }
                val bytes = connection.inputStream.use { input ->
                    val buffer = ByteArray(1025)
                    var length = 0
                    while (length < buffer.size) {
                        val read = input.read(buffer, length, buffer.size - length)
                        if (read == -1) break
                        length += read
                    }
                    check(length <= 1024) { "Checksum file exceeds its size limit" }
                    buffer.copyOf(length)
                }
                return@withContext parseChecksumContent(bytes.toString(Charsets.UTF_8))
                    ?: error("Invalid SHA-256 checksum file")
            } finally {
                connection.disconnect()
            }
        }
        error("Checksum redirect limit exceeded")
    }
}
