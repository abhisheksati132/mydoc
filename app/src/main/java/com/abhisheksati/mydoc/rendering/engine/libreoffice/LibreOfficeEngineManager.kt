package com.abhisheksati.mydoc.rendering.engine.libreoffice

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libreoffice.kit.LibreOfficeKit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed interface EngineStatus {
    data object NotInstalled : EngineStatus
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L
    ) : EngineStatus
    data object Extracting : EngineStatus
    data class Ready(
        val version: String,
        val installSizeBytes: Long,
        val installPath: String
    ) : EngineStatus
    data class Error(val message: String) : EngineStatus
}

class LibreOfficeEngineManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.NotInstalled)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val engineDir = File(context.filesDir, "libreoffice_engine")
    private val versionFile = File(engineDir, "engine_version.txt")

    init {
        checkInstalledState()
    }

    companion object {
        private const val TAG = "LibreOfficeEngineMgr"
        private const val DEFAULT_VERSION = "26.2.6.3"

        // Official F-Droid packages by architecture
        private val FDROID_ABI_URLS = mapOf(
            "arm64-v8a" to "https://f-droid.org/repo/org.documentfoundation.libreoffice_131.apk",
            "armeabi-v7a" to "https://f-droid.org/repo/org.documentfoundation.libreoffice_130.apk",
            "x86_64" to "https://f-droid.org/repo/org.documentfoundation.libreoffice_129.apk",
            "x86" to "https://f-droid.org/repo/org.documentfoundation.libreoffice_128.apk"
        )

        // Resilient global mirrors
        private val MIRROR_ABI_URLS = mapOf(
            "arm64-v8a" to listOf(
                "https://f-droid.org/repo/org.documentfoundation.libreoffice_131.apk",
                "https://mirror.cyberbits.eu/fdroid/repo/org.documentfoundation.libreoffice_131.apk",
                "https://ftp.osuosl.org/pub/fdroid/repo/org.documentfoundation.libreoffice_131.apk"
            ),
            "armeabi-v7a" to listOf(
                "https://f-droid.org/repo/org.documentfoundation.libreoffice_130.apk",
                "https://mirror.cyberbits.eu/fdroid/repo/org.documentfoundation.libreoffice_130.apk",
                "https://ftp.osuosl.org/pub/fdroid/repo/org.documentfoundation.libreoffice_130.apk"
            ),
            "x86_64" to listOf(
                "https://f-droid.org/repo/org.documentfoundation.libreoffice_129.apk",
                "https://mirror.cyberbits.eu/fdroid/repo/org.documentfoundation.libreoffice_129.apk",
                "https://ftp.osuosl.org/pub/fdroid/repo/org.documentfoundation.libreoffice_129.apk"
            ),
            "x86" to listOf(
                "https://f-droid.org/repo/org.documentfoundation.libreoffice_128.apk",
                "https://mirror.cyberbits.eu/fdroid/repo/org.documentfoundation.libreoffice_128.apk",
                "https://ftp.osuosl.org/pub/fdroid/repo/org.documentfoundation.libreoffice_128.apk"
            )
        )

        @Volatile
        private var INSTANCE: LibreOfficeEngineManager? = null

        fun getInstance(context: Context): LibreOfficeEngineManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LibreOfficeEngineManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getDevicePrimaryAbi(): String {
            return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        }

        fun getSuggestedDownloadUrl(abi: String = getDevicePrimaryAbi()): String {
            return FDROID_ABI_URLS[abi] ?: FDROID_ABI_URLS["arm64-v8a"]!!
        }
    }

    fun isReady(): Boolean {
        return _status.value is EngineStatus.Ready && engineDir.exists() && versionFile.exists()
    }

    fun hasEngineFilesOnDisk(): Boolean {
        return engineDir.exists() && (getFolderSize(engineDir) > 0L || versionFile.exists())
    }

    fun getDiskSpaceUsed(): Long {
        return getFolderSize(engineDir)
    }

    fun checkInstalledState() {
        if (engineDir.exists() && versionFile.exists()) {
            val version = runCatching { versionFile.readText().trim() }.getOrDefault(DEFAULT_VERSION)
            val size = getFolderSize(engineDir)
            if (size > 1024 * 1024) { // At least 1MB
                // Only mark Ready on disk. DO NOT eagerly initialize native C++ on startup!
                _status.value = EngineStatus.Ready(
                    version = version,
                    installSizeBytes = size,
                    installPath = engineDir.absolutePath
                )
                return
            }
        }
        _status.value = EngineStatus.NotInstalled
    }

    fun initNativeEngine(): Boolean {
        if (LibreOfficeKit.isInitialized()) return true
        if (!engineDir.exists()) return false

        val marker = File(context.filesDir, "libreoffice_init.marker")
        return try {
            marker.createNewFile()
            val ok = LibreOfficeKit.init(context, engineDir)
            if (marker.exists()) marker.delete()
            ok
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize native LibreOfficeKit", t)
            if (marker.exists()) marker.delete()
            false
        }
    }

    fun downloadAndInstall(
        customUrl: String? = null,
        onComplete: (() -> Unit)? = null
    ) {
        if (_status.value is EngineStatus.Downloading || _status.value is EngineStatus.Extracting) {
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch {
            val primaryAbi = getDevicePrimaryAbi()
            val candidateUrls = if (!customUrl.isNullOrBlank()) {
                listOf(customUrl.trim())
            } else {
                MIRROR_ABI_URLS[primaryAbi] ?: listOf(getSuggestedDownloadUrl(primaryAbi))
            }
            val tempZipFile = File(context.cacheDir, "libreoffice_engine_download.apk")

            try {
                _status.value = EngineStatus.Downloading(
                    progressPercent = 0,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    speedBytesPerSec = 0L
                )

                // 1. Download stream with mirror fallback
                var downloadSuccess = false
                var lastErr: Throwable? = null
                for (url in candidateUrls) {
                    try {
                        Log.i(TAG, "Attempting engine download from: $url")
                        downloadFileWithProgress(url, tempZipFile)
                        downloadSuccess = true
                        break
                    } catch (t: Throwable) {
                        lastErr = t
                        Log.w(TAG, "Mirror download failed for $url: ${t.message}")
                    }
                }

                if (!downloadSuccess) {
                    throw lastErr ?: IllegalStateException("Unable to download from available mirrors")
                }

                // 2. Extract into target directory
                _status.value = EngineStatus.Extracting
                extractZipArchive(tempZipFile, engineDir)

                // 3. Mark version
                versionFile.writeText(DEFAULT_VERSION)

                // Clean up temp file
                if (tempZipFile.exists()) tempZipFile.delete()

                val installedSize = getFolderSize(engineDir)
                _status.value = EngineStatus.Ready(
                    version = DEFAULT_VERSION,
                    installSizeBytes = installedSize,
                    installPath = engineDir.absolutePath
                )
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            } catch (e: CancellationException) {
                if (tempZipFile.exists()) tempZipFile.delete()
                checkInstalledState()
            } catch (t: Throwable) {
                Log.e(TAG, "Engine download/install failed", t)
                if (tempZipFile.exists()) tempZipFile.delete()
                _status.value = EngineStatus.Error(
                    t.localizedMessage ?: "Failed to download LibreOffice engine bundle"
                )
            }
        }
    }

    fun installFromZipUri(
        zipUri: Uri,
        onComplete: (() -> Unit)? = null
    ) {
        if (_status.value is EngineStatus.Downloading || _status.value is EngineStatus.Extracting) {
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch {
            _status.value = EngineStatus.Extracting
            val tempZip = File(context.cacheDir, "imported_lo_engine.zip")
            try {
                context.contentResolver.openInputStream(zipUri)?.use { input ->
                    FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Could not open input stream for zip")

                extractZipArchive(tempZip, engineDir)
                versionFile.writeText(DEFAULT_VERSION)
                if (tempZip.exists()) tempZip.delete()

                val installedSize = getFolderSize(engineDir)
                _status.value = EngineStatus.Ready(
                    version = DEFAULT_VERSION,
                    installSizeBytes = installedSize,
                    installPath = engineDir.absolutePath
                )
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Importing engine zip failed", t)
                if (tempZip.exists()) tempZip.delete()
                _status.value = EngineStatus.Error(
                    "Import failed: ${t.localizedMessage ?: "Invalid engine archive"}"
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val tempZip = File(context.cacheDir, "libreoffice_engine_download.zip")
        if (tempZip.exists()) tempZip.delete()
        checkInstalledState()
    }

    fun uninstall(onComplete: (() -> Unit)? = null) {
        downloadJob?.cancel()
        downloadJob = null
        scope.launch {
            try {
                if (engineDir.exists()) {
                    deleteRecursively(engineDir)
                }
                val tempZip1 = File(context.cacheDir, "libreoffice_engine_download.zip")
                if (tempZip1.exists()) tempZip1.delete()
                val tempZip2 = File(context.cacheDir, "imported_lo_engine.zip")
                if (tempZip2.exists()) tempZip2.delete()
                val marker = File(context.filesDir, "libreoffice_init.marker")
                if (marker.exists()) marker.delete()

                LibreOfficeKit.reset()
                _status.value = EngineStatus.NotInstalled
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to uninstall LibreOffice engine", t)
                _status.value = EngineStatus.Error("Failed to delete files: ${t.localizedMessage}")
            }
        }
    }

    private suspend fun downloadFileWithProgress(urlStr: String, destinationFile: File) {
        withContext(Dispatchers.IO) {
            var currentUrl = urlStr
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 5

            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 45000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) MyDoc-LibreOfficeEngine/1.0")
                }

                val responseCode = conn.responseCode
                if (responseCode in setOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                        redirects++
                        continue
                    }
                }

                if (responseCode !in 200..299) {
                    conn.disconnect()
                    throw IllegalStateException("Server returned HTTP $responseCode: ${conn.responseMessage}")
                }

                connection = conn
                break
            }

            val finalConn = connection ?: throw IllegalStateException("Could not establish connection to $urlStr")
            val contentLength = finalConn.contentLengthLong
            val inputStream = BufferedInputStream(finalConn.inputStream, 32768)
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(32768)
            var totalRead = 0L
            var lastUpdateTimestamp = System.currentTimeMillis()
            var bytesReadSinceLastUpdate = 0L
            var currentSpeed = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        bytesReadSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        val diff = now - lastUpdateTimestamp
                        if (diff >= 300) {
                            currentSpeed = (bytesReadSinceLastUpdate * 1000) / diff
                            lastUpdateTimestamp = now
                            bytesReadSinceLastUpdate = 0L

                            val percent = if (contentLength > 0) {
                                ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }

                            _status.value = EngineStatus.Downloading(
                                progressPercent = percent,
                                downloadedBytes = totalRead,
                                totalBytes = contentLength,
                                speedBytesPerSec = currentSpeed
                            )
                        }
                    }
                }
            }
        }
    }

    private fun extractZipArchive(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // Keep a copy of the core package as libreoffice_core.apk for asset resolution
        val targetApk = File(targetDir, "libreoffice_core.apk")
        try {
            zipFile.copyTo(targetApk, overwrite = true)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy core package archive: ${e.message}")
        }

        val targetCanonical = targetDir.canonicalPath
        val libDir = File(targetDir, "lib")
        if (!libDir.exists()) {
            libDir.mkdirs()
        }

        ZipInputStream(BufferedInputStream(zipFile.inputStream(), 32768)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(32768)

            while (entry != null) {
                val name = entry.name
                val isLib = name.startsWith("lib/") && name.endsWith(".so")
                val isAsset = name.startsWith("assets/") && !name.contains("dexopt")

                // Extract native libraries, registry assets, and configs
                if (isLib || isAsset || name.endsWith(".so") || name.endsWith(".rdb") || name.endsWith(".rc")) {
                    val newFile = File(targetDir, name)
                    // Prevent Zip Slip vulnerability
                    if (!newFile.canonicalPath.startsWith(targetCanonical)) {
                        throw SecurityException("Zip entry is outside of target directory: $name")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var count: Int
                            while (zis.read(buffer).also { count = it } != -1) {
                                fos.write(buffer, 0, count)
                            }
                        }
                        if (newFile.name.endsWith(".so")) {
                            newFile.setReadable(true, false)
                            newFile.setExecutable(true, false)

                            // Also place directly into lib/ for immediate linker discovery if primary ABI
                            val primaryAbi = getDevicePrimaryAbi()
                            val isPrimaryAbi = name.startsWith("lib/$primaryAbi/") || !name.contains("/")
                            if (isPrimaryAbi) {
                                val directLib = File(libDir, newFile.name)
                                if (directLib.canonicalPath != newFile.canonicalPath) {
                                    try {
                                        newFile.copyTo(directLib, overwrite = true)
                                        directLib.setReadable(true, false)
                                        directLib.setExecutable(true, false)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += getFolderSize(child)
        }
        return size
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }

    fun isSupportedFormat(extension: String): Boolean {
        val ext = extension.lowercase().removePrefix(".")
        return ext in setOf(
            "doc", "docx", "dot", "dotx", "rtf", "odt", "fodt", "ott",
            "xls", "xlsx", "xlt", "xltx", "ods", "fods", "ots", "csv", "tsv",
            "ppt", "pptx", "pot", "potx", "pps", "ppsx", "odp", "fodp", "otp",
            "wps", "wpd"
        )
    }
}
