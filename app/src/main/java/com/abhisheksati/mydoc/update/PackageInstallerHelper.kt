package com.abhisheksati.mydoc.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

class PackageInstallerHelper(private val context: Context) {

    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createManageUnknownAppSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun installApk(apkFile: File) {
        verifyApkPackageAndSignature(apkFile)

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
        for (resolveInfo in resInfoList) {
            val pkg = resolveInfo.activityInfo.packageName
            context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
    }

    fun verifyApkPackageAndSignature(apkFile: File) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            android.content.pm.PackageManager.GET_SIGNATURES
        }

        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: throw SecurityException("Verification failed: Cannot parse package archive info for ${apkFile.name}")

        if (archiveInfo.packageName != context.packageName) {
            throw SecurityException(
                "Verification failed: Package name mismatch! Expected '${context.packageName}', got '${archiveInfo.packageName}'"
            )
        }

        val currentAppInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
        }

        val currentSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            currentAppInfo.signingInfo?.signingCertificateHistory?.map { it.toByteArray() }
                ?: currentAppInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }
                ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            currentAppInfo.signatures?.map { it.toByteArray() } ?: emptyList()
        }

        val newSignatures: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.signingInfo?.signingCertificateHistory?.map { it.toByteArray() }
                ?: archiveInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }
                ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.signatures?.map { it.toByteArray() } ?: emptyList()
        }

        // If the current app has signatures, ensure the new APK matches at least one signing certificate
        if (currentSignatures.isNotEmpty() && newSignatures.isNotEmpty()) {
            val hasMatch = newSignatures.any { newSig ->
                currentSignatures.any { currSig -> currSig.contentEquals(newSig) }
            }
            if (!hasMatch) {
                throw SecurityException("Verification failed: APK signing certificate does not match the installed app certificate.")
            }
        }
    }
}
