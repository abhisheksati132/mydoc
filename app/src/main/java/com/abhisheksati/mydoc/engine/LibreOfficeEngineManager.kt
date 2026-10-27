package com.abhisheksati.mydoc.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class LibreOfficeStatus {
    object NotInstalled : LibreOfficeStatus()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : LibreOfficeStatus()
    data class Downloaded(val file: File, val sizeMb: Float) : LibreOfficeStatus()
    data class Installed(val versionName: String, val packageName: String) : LibreOfficeStatus()
}

/**
 * Manages the on-demand LibreOffice document engine for Android.
 *
 * Provides:
 * - Real-time status detection (Installed / Downloaded / Downloading / Not Installed).
 * - Background APK downloading with live progress from verified official mirrors.
 * - System package installation intent invocation.
 * - APK deletion to reclaim storage.
 * - Secure Intent dispatch to view Word, Excel, PowerPoint, and OpenDocument files.
 */
object LibreOfficeEngineManager {

    val SUPPORTED_PACKAGES = listOf(
        "org.documentfoundation.libreoffice",
        "com.collabora.libreoffice"
    )

    // Verified official LibreOffice Viewer direct APK mirrors (F-Droid builds)
    private val DOWNLOAD_CANDIDATES = listOf(
        "https://f-droid.org/repo/org.documentfoundation.libreoffice_131.apk",
        "https://f-droid.org/repo/org.documentfoundation.libreoffice_130.apk",
        "https://f-droid.org/repo/org.documentfoundation.libreoffice_129.apk",
        "https://f-droid.org/repo/org.documentfoundation.libreoffice_128.apk"
    )

    private const val APK_FILE_NAME = "libreoffice-engine.apk"

    private val _status = MutableStateFlow<LibreOfficeStatus>(LibreOfficeStatus.NotInstalled)
    val status: StateFlow<LibreOfficeStatus> = _status.asStateFlow()

    /**
     * Refresh the current status of the engine.
     */
    fun refreshStatus(context: Context) {
        val installedPkg = findInstalledPackage(context)
        if (installedPkg != null) {
            val vName = getPackageVersion(context, installedPkg) ?: "Installed"
            _status.value = LibreOfficeStatus.Installed(vName, installedPkg)
            return
        }

        val apk = getDownloadedApkFile(context)
        if (apk != null && apk.exists() && apk.length() > 5 * 1024 * 1024) {
            val sizeMb = apk.length() / (1024f * 1024f)
            _status.value = LibreOfficeStatus.Downloaded(apk, sizeMb)
        } else {
            if (_status.value !is LibreOfficeStatus.Downloading) {
                _status.value = LibreOfficeStatus.NotInstalled
            }
        }
    }

    fun isInstalled(context: Context): Boolean {
        return findInstalledPackage(context) != null
    }

    fun findInstalledPackage(context: Context): String? {
        val pm = context.packageManager
        for (pkg in SUPPORTED_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: PackageManager.NameNotFoundException) {
                // not installed
            }
        }
        return null
    }

    private fun getPackageVersion(context: Context, packageName: String): String? {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
    }

    fun getDownloadedApkFile(context: Context): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = File(dir, APK_FILE_NAME)
        return if (file.exists()) file else null
    }

    /**
     * Downloads the official LibreOffice APK with live progress reporting across candidate mirrors.
     */
    suspend fun startDownload(
        context: Context,
        onComplete: ((File) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val targetFile = File(targetDir, APK_FILE_NAME)
        val tempFile = File(targetDir, "$APK_FILE_NAME.tmp")

        _status.value = LibreOfficeStatus.Downloading(0, 0L, 0L)

        var lastError: String? = null
        var downloadSucceeded = false

        for (candidateUrl in DOWNLOAD_CANDIDATES) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(candidateUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 MyDoc-Android")
                }
                connection.connect()

                val code = connection.responseCode
                if (code !in 200..299) {
                    lastError = "HTTP $code from mirror"
                    continue
                }

                val totalBytes = connection.contentLength.toLong()
                var bytesDownloaded = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var lastReportedPercent = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead

                            val percent = if (totalBytes > 0) {
                                ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }

                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                _status.value = LibreOfficeStatus.Downloading(percent, bytesDownloaded, totalBytes)
                            }
                        }
                        output.flush()
                    }
                }

                if (tempFile.exists() && tempFile.length() > 5 * 1024 * 1024) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    downloadSucceeded = true
                    break
                }
            } catch (e: Exception) {
                lastError = e.message
            } finally {
                connection?.disconnect()
            }
        }

        if (downloadSucceeded && targetFile.exists()) {
            val sizeMb = targetFile.length() / (1024f * 1024f)
            _status.value = LibreOfficeStatus.Downloaded(targetFile, sizeMb)

            withContext(Dispatchers.Main) {
                onComplete?.invoke(targetFile)
            }
        } else {
            tempFile.delete()
            _status.value = LibreOfficeStatus.NotInstalled
            withContext(Dispatchers.Main) {
                onError?.invoke(lastError ?: "Failed to download LibreOffice engine")
            }
        }
    }

    /**
     * Prompts Android package installer to install the downloaded APK.
     */
    fun installDownloadedApk(context: Context): Boolean {
        val apk = getDownloadedApkFile(context)
        if (apk == null || !apk.exists()) {
            Toast.makeText(context, "Installer APK not found. Please download again.", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apk)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Deletes the downloaded APK installer to free storage space.
     */
    fun deleteDownloadedApk(context: Context): Boolean {
        val apk = getDownloadedApkFile(context)
        val deleted = apk?.delete() ?: false
        refreshStatus(context)
        return deleted
    }

    /**
     * Launches an office document in LibreOffice.
     */
    fun launchOfficeDocument(context: Context, fileUri: Uri, mimeType: String): Boolean {
        val targetPkg = findInstalledPackage(context)

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType.ifEmpty { "application/octet-stream" })
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                if (targetPkg != null) {
                    setPackage(targetPkg)
                }
            }

            if (targetPkg != null) {
                context.grantUriPermission(targetPkg, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to general intent chooser if specific package launch fails
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, mimeType.ifEmpty { "application/octet-stream" })
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Open with").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Opens Google Play Store or F-Droid for LibreOffice Viewer.
     */
    fun openStorePage(context: Context) {
        val marketUri = Uri.parse("market://details?id=org.documentfoundation.libreoffice")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=org.documentfoundation.libreoffice")

        try {
            val intent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Opens Android System App Info so the user can manage or uninstall LibreOffice.
     */
    fun openAppDetails(context: Context) {
        val pkg = findInstalledPackage(context) ?: "org.documentfoundation.libreoffice"
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", pkg, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open app settings", Toast.LENGTH_SHORT).show()
        }
    }
}
