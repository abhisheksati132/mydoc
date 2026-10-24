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

data class AppInfoItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val apkFile: File,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val installTime: Long,
    val icon: Drawable?
)

class AppManagerHelper(private val context: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<AppInfoItem>()
        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val info = activityInfo.applicationInfo ?: continue
            if (!seenPackages.add(info.packageName)) continue

            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystemApps && isSystem) {
                continue
            }

            val apkFile = File(info.sourceDir)
            if (!apkFile.exists()) continue

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
                    apkSize = apkFile.length(),
                    isSystemApp = isSystem,
                    installTime = installTime,
                    icon = icon
                )
            )
        }

        appList.sortedBy { it.appName.lowercase() }
    }

    suspend fun extractApk(app: AppInfoItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Stacks/ExtractedAPKs"
            )
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val sanitizedName = app.appName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(targetDir, "${sanitizedName}_v${app.versionName}.apk")

            FileInputStream(app.apkFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
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
