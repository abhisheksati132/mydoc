package com.abhisheksati.mydoc.rendering.engine

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.abhisheksati.mydoc.BuildConfig
import com.abhisheksati.mydoc.update.ApkDownloader
import com.abhisheksati.mydoc.update.PackageInstallerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/** The optional engine is a signed Android package; no downloaded code runs in MyDoc. */
object OfficeEngineManager {
    private val downloads = Mutex()
    const val VERSION = "26.2.6.3"
    const val PACKAGE_VERSION = "$VERSION-1"
    const val PROTOCOL = 1
    const val SERVICE = "com.abhisheksati.mydoc.rendering.engine.word.WordEngineService"
    fun packageName(context: Context) = "com.abhisheksati.mydoc.officeengine" + if (BuildConfig.DEBUG) ".debug" else ""
    fun component(context: Context) = ComponentName(packageName(context), SERVICE)

    @Suppress("DEPRECATION")
    fun isInstalled(context: Context): Boolean = runCatching {
        val pm = context.packageManager
        val service = pm.getServiceInfo(component(context), PackageManager.GET_META_DATA)
        service.enabled && service.applicationInfo.enabled &&
            pm.getPackageInfo(packageName(context), 0).versionName == PACKAGE_VERSION &&
            service.metaData?.getInt("mydoc.office.protocol") == PROTOCOL &&
            pm.checkSignatures(context.packageName, packageName(context)) == PackageManager.SIGNATURE_MATCH
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    fun installedBytes(context: Context): Long = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName(context), 0)
        (listOfNotNull(info.sourceDir) + info.splitSourceDirs.orEmpty()).sumOf { File(it).length() } +
            File(info.nativeLibraryDir).listFiles().orEmpty().sumOf { it.length() }
    }.getOrDefault(0L)

    suspend fun download(context: Context, onProgress: (Long, Long) -> Unit): File = downloads.withLock { withContext(Dispatchers.IO) {
        check(!BuildConfig.DEBUG) { "For debug builds, install the matching office-engine debug APK from the CI artifacts." }
        val abi = Build.SUPPORTED_ABIS.firstOrNull { it in setOf("arm64-v8a", "armeabi-v7a", "x86_64") }
            ?: error("This device architecture is not supported")
        val name = "mydoc-office-engine-$abi.apk"
        val url = "https://github.com/abhisheksati132/mydoc/releases/download/office-engine-$PACKAGE_VERSION/$name"
        val downloader = ApkDownloader(context, "office-engine-download")
        val apk = try {
            val checksum = downloader.fetchChecksumText("$url.sha256")
            downloader.downloadApk(url, name, checksum, onProgress).getOrThrow()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (primary: Exception) {
            try {
                val (id, checksum) = findEngineAsset(name)
                downloader.downloadApk(
                    "https://api.github.com/repos/abhisheksati132/mydoc/releases/assets/$id",
                    name, checksum, onProgress, accept = "application/octet-stream"
                ).getOrThrow()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (fallback: Exception) {
                fallback.addSuppressed(primary)
                throw IllegalStateException("Could not download and verify the Office engine from GitHub. Try again.", fallback)
            }
        }
        try {
            verifyArchive(context, apk)
            apk
        } catch (error: Throwable) { apk.delete(); throw error }
    } }

    private fun findEngineAsset(name: String): Pair<Long, String> {
        val tag = "office-engine-$PACKAGE_VERSION"
        val endpoint = "https://api.github.com/repos/abhisheksati132/mydoc/releases/tags/$tag"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "mydoc-android-updater")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Office engine metadata is unavailable" }
            val release = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            check(release.getString("tag_name") == tag && !release.getBoolean("draft"))
            val assets = release.getJSONArray("assets")
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.getString("name") != name || asset.optString("state") != "uploaded") continue
                val id = asset.getLong("id")
                val digest = asset.getString("digest")
                check(id > 0 && digest.startsWith("sha256:"))
                val checksum = requireNotNull(ApkDownloader.parseChecksumContent(digest.removePrefix("sha256:")))
                return id to checksum
            }
            error("No Office engine APK found for this device")
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyArchive(context: Context, file: File) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(file.path, flags) ?: error("Invalid Office engine APK")
        check(archive.packageName == packageName(context) && archive.versionName == PACKAGE_VERSION) {
            "The downloaded Office engine is not compatible with this MyDoc version"
        }
        val current = PackageInstallerHelper.extractSignatures(pm.getPackageInfo(context.packageName, flags))
        val signatures = PackageInstallerHelper.extractSignatures(archive)
        check(signatures.isNotEmpty() && signatures.all { candidate -> current.any { it.contentEquals(candidate) } }) {
            "The Office engine signing certificate does not match MyDoc"
        }
        PackageInstallerHelper.verifyPinnedCertificate(BuildConfig.DEBUG, BuildConfig.RELEASE_CERT_SHA256, signatures)
    }
}
