package com.abhisheksati.mydoc.tools.appmanager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.abhisheksati.mydoc.util.copyBounded

data class AppInfoItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val apkFile: File,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val installTime: Long,
    val icon: Drawable?,
    val isSplitApk: Boolean = false,
    val splitApkFiles: List<File> = emptyList()
)

class AppManagerHelper(private val context: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val applicationInfos: List<ApplicationInfo> = try {
            if (includeSystemApps) {
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            } else {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
                if (resolveInfos.isNotEmpty()) {
                    resolveInfos.mapNotNull { it.activityInfo?.applicationInfo }
                } else {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                }
            }
        } catch (_: Exception) {
            runCatching { pm.getInstalledApplications(0) }.getOrDefault(emptyList())
        }

        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<AppInfoItem>()
        for (info in applicationInfos) {
            if (!seenPackages.add(info.packageName)) continue

            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystemApps && isSystem) {
                continue
            }

            val apkFile = File(info.sourceDir)
            if (!apkFile.exists()) continue

            val splitDirs = info.splitSourceDirs ?: info.splitPublicSourceDirs
            val splitFiles = splitDirs?.map { File(it) }?.filter { it.exists() } ?: emptyList()
            val isSplit = splitFiles.isNotEmpty()
            val totalSize = apkFile.length() + splitFiles.sumOf { it.length() }

            val appName = try {
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                info.packageName
            }

            val pkgInfo = try {
                pm.getPackageInfo(info.packageName, 0)
            } catch (_: Exception) {
                null
            }

            val versionName = pkgInfo?.versionName ?: "1.0"
            val installTime = pkgInfo?.firstInstallTime ?: System.currentTimeMillis()
            val icon = try {
                pm.getApplicationIcon(info)
            } catch (_: Exception) {
                null
            }

            appList.add(
                AppInfoItem(
                    appName = appName,
                    packageName = info.packageName,
                    versionName = versionName,
                    apkFile = apkFile,
                    apkSize = totalSize,
                    isSystemApp = isSystem,
                    installTime = installTime,
                    icon = icon,
                    isSplitApk = isSplit,
                    splitApkFiles = splitFiles
                )
            )
        }

        appList.sortedBy { it.appName.lowercase() }
    }

    suspend fun extractApk(app: AppInfoItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val primaryDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MyDoc/ExtractedAPKs"
            )
            val targetDir = if (primaryDir.exists() || primaryDir.mkdirs()) {
                primaryDir
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "ExtractedAPKs").apply { mkdirs() }
            }

            val sanitizedName = app.appName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val sanitizedVersion = app.versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            // The package is part of the name: two installations can share a label and version
            // (a patch, a beta channel) and used to collide on one export path.
            val baseName = "${sanitizedName}_${app.packageName.replace('.', '_')}_v$sanitizedVersion"
            if (app.isSplitApk && app.splitApkFiles.isNotEmpty()) {
                val allApks = listOf(app.apkFile) + app.splitApkFiles
                for (apk in allApks) {
                    if (!apk.exists() || !apk.canRead()) {
                        throw java.io.FileNotFoundException("Required APK component not found or unreadable: ${apk.name}")
                    }
                }
                val target = writeStaged(targetDir, baseName, "apks") { staging ->
                    ZipOutputStream(FileOutputStream(staging)).use { zos ->
                        val infoJson = org.json.JSONObject().apply {
                            put("pckg", app.packageName)
                            put("version_name", app.versionName)
                            put("app_name", app.appName)
                            val splitsArray = org.json.JSONArray()
                            for (apk in allApks) {
                                splitsArray.put(apk.name)
                            }
                            put("splits", splitsArray)
                        }
                        zos.putNextEntry(ZipEntry("info.json"))
                        zos.write(infoJson.toString(2).toByteArray(Charsets.UTF_8))
                        zos.closeEntry()

                        for (apk in allApks) {
                            zos.putNextEntry(ZipEntry(apk.name))
                            FileInputStream(apk).use { it.copyBounded(zos) }
                            zos.closeEntry()
                        }
                    }
                }
                android.media.MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
                Result.success(target)
            } else {
                if (!app.apkFile.exists() || !app.apkFile.canRead()) {
                    throw java.io.FileNotFoundException("Required APK component not found or unreadable: ${app.apkFile.name}")
                }
                val target = writeStaged(targetDir, baseName, "apk") { staging ->
                    FileInputStream(app.apkFile).use { input ->
                        FileOutputStream(staging).use { output ->
                            input.copyBounded(output)
                        }
                    }
                }
                android.media.MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
                Result.success(target)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes through a unique staging file and only then publishes it, so a failed export leaves
     * no half-written artifact under the real name and never truncates an earlier export.
     */
    private fun writeStaged(dir: File, baseName: String, extension: String, write: (File) -> Unit): File {
        val staging = File.createTempFile("$baseName.", ".$extension.tmp", dir)
        try {
            write(staging)
            val target = uniqueTarget(dir, "$baseName.$extension")
            if (!staging.renameTo(target)) {
                staging.copyTo(target, overwrite = false)
                staging.delete()
            }
            return target
        } catch (t: Throwable) {
            staging.delete()
            throw t
        }
    }

    private fun uniqueTarget(dir: File, name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val stem = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        var index = 2
        while (true) {
            val candidate = File(dir, if (ext.isEmpty()) "$stem ($index)" else "$stem ($index).$ext")
            if (!candidate.exists()) return candidate
            index++
        }
    }

    fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
