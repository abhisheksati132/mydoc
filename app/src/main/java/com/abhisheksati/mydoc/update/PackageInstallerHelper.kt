package com.abhisheksati.mydoc.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.BuildConfig
import java.io.File
import java.security.MessageDigest
import java.util.Locale

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
            clipData = ClipData.newRawUri("mydoc_apk", apkUri)
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
            ?: pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: throw SecurityException("Verification failed: Cannot parse package archive info for ${apkFile.name}")

        if (archiveInfo.packageName != context.packageName) {
            throw SecurityException(
                "Verification failed: Package name mismatch! Expected '${context.packageName}', got '${archiveInfo.packageName}'"
            )
        }

        val currentAppInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }
        } catch (e: Exception) {
            throw SecurityException("Verification failed: Cannot read installed package info for '${context.packageName}'", e)
        } ?: throw SecurityException("Verification failed: Installed package info is null for '${context.packageName}'")

        val currentSignatures: List<ByteArray> = extractSignatures(currentAppInfo)
        val newSignatures: List<ByteArray> = extractSignatures(archiveInfo)

        if (currentSignatures.isEmpty()) {
            throw SecurityException("Verification failed: Installed app has no valid signatures")
        }
        if (newSignatures.isEmpty()) {
            throw SecurityException("Verification failed: Downloaded APK archive has no valid signatures")
        }

        // 1. Installed signature compatibility check
        val matchesInstalled = newSignatures.any { newSig ->
            currentSignatures.any { currSig -> currSig.contentEquals(newSig) }
        }
        if (!matchesInstalled) {
            throw SecurityException(
                "Cannot update automatically: The downloaded APK certificate differs from this installed build " +
                        "(e.g. debug vs release keystore). Please install the APK manually or uninstall the debug build first."
            )
        }

        // 2. Pinned certificate check (Mandatory for release builds)
        verifyPinnedCertificate(BuildConfig.DEBUG, BuildConfig.RELEASE_CERT_SHA256, newSignatures)
    }

    companion object {
        fun verifyPinnedCertificate(
            isDebugBuild: Boolean,
            pinnedFingerprintsString: String,
            newSignatures: List<ByteArray>
        ) {
            if (newSignatures.isEmpty()) {
                throw SecurityException("Verification failed: Signatures list cannot be empty")
            }
            if (isDebugBuild) {
                return
            }

            val pinnedFingerprints = pinnedFingerprintsString
                .split(',')
                .map { it.trim().replace(":", "").lowercase(Locale.ROOT) }
                .filter { it.isNotBlank() }

            if (pinnedFingerprints.isEmpty()) {
                throw SecurityException(
                    "Verification failed: Pinned release certificate fingerprint list (RELEASE_CERT_SHA256) is empty in release build. Installation refused."
                )
            }

            val newSignaturesSha256 = newSignatures.map { computeCertSha256(it) }
            val matchesPinned = newSignaturesSha256.any { newCertSha ->
                pinnedFingerprints.contains(newCertSha)
            }

            if (!matchesPinned) {
                throw SecurityException(
                    "Verification failed: Downloaded APK certificate ($newSignaturesSha256) does not match any pinned release certificate ($pinnedFingerprints)."
                )
            }
        }
        fun extractSignatures(
            packageInfo: android.content.pm.PackageInfo
        ): List<ByteArray> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
                    } else {
                        val history = signingInfo.signingCertificateHistory?.map { it.toByteArray() }
                        if (!history.isNullOrEmpty()) history
                        else signingInfo.apkContentsSigners?.map { it.toByteArray() } ?: emptyList()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures?.map { it.toByteArray() } ?: emptyList()
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }
        }

        fun computeCertSha256(certBytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(certBytes)
            return digest.joinToString("") { "%02x".format(it) }.lowercase(Locale.ROOT)
        }
    }
}
