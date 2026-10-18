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
        private const val DEFAULT_VERSION = "7.6.4-core"
        private const val GITHUB_REPO_URL = "https://github.com/abhisheksati132/mydoc/releases/download/v1.0.0-engine"

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
    }

    fun isReady(): Boolean {
        return _status.value is EngineStatus.Ready && LibreOfficeKit.isAvailable()
    }

    fun checkInstalledState() {
        if (engineDir.exists() && versionFile.exists()) {
            val version = runCatching { versionFile.readText().trim() }.getOrDefault(DEFAULT_VERSION)
            val size = getFolderSize(engineDir)
            if (size > 1024 * 1024) { // At least 1MB
                // Try initializing if not already done
                initNativeEngine()
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
        return try {
            if (!engineDir.exists()) return false
            LibreOfficeKit.init(context, engineDir)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize native LibreOfficeKit", t)
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
            val downloadUrl = customUrl ?: "$GITHUB_REPO_URL/libreoffice-engine-$primaryAbi.zip"
            val tempZipFile = File(context.cacheDir, "libreoffice_engine_download.zip")

            try {
                _status.value = EngineStatus.Downloading(
                    progressPercent = 0,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    speedBytesPerSec = 0L
                )

                // 1. Download stream
                downloadFileWithProgress(downloadUrl, tempZipFile)

                // 2. Extract into target directory
                _status.value = EngineStatus.Extracting
                extractZipArchive(tempZipFile, engineDir)

                // 3. Mark version
                versionFile.writeText(DEFAULT_VERSION)

                // Clean up temp zip
                if (tempZipFile.exists()) tempZipFile.delete()

                // 4. Initialize native engine
                initNativeEngine()

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

                initNativeEngine()
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

    fun uninstall() {
        downloadJob?.cancel()
        downloadJob = null
        scope.launch {
            try {
                if (engineDir.exists()) {
                    deleteRecursively(engineDir)
                }
                LibreOfficeKit.reset()
                _status.value = EngineStatus.NotInstalled
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to uninstall LibreOffice engine", t)
                _status.value = EngineStatus.Error("Failed to delete files: ${t.localizedMessage}")
            }
        }
    }

    private suspend fun downloadFileWithProgress(urlStr: String, destinationFile: File) {
        withContext(Dispatchers.IO) {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MyDoc-LibreOfficeEngineDownloader/1.0")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Server returned HTTP $responseCode: ${connection.responseMessage}")
            }

            val contentLength = connection.contentLengthLong
            val inputStream = BufferedInputStream(connection.inputStream, 32768)
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
                        if (diff >= 400) {
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

        val targetCanonical = targetDir.canonicalPath
        ZipInputStream(BufferedInputStream(zipFile.inputStream(), 32768)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(32768)

            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                // Prevent Zip Slip vulnerability
                if (!newFile.canonicalPath.startsWith(targetCanonical)) {
                    throw SecurityException("Zip entry is outside of target directory: ${entry.name}")
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
