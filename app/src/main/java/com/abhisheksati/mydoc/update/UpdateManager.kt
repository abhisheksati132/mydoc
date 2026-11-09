package com.abhisheksati.mydoc.update

import android.content.Context
import android.content.pm.PackageManager
import com.abhisheksati.mydoc.BuildConfig
import com.abhisheksati.mydoc.domain.model.AppRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    data class NoUpdate(val currentVersion: String, val latestRelease: AppRelease? = null) : UpdateUiState
    data class UpdateAvailable(val release: AppRelease, val currentVersion: String) : UpdateUiState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateUiState
    data class ReadyToInstall(val apkFile: File, val release: AppRelease) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateManager(
    private val context: Context,
    private val updateService: GitHubUpdateService = GitHubUpdateService(),
    private val apkDownloader: ApkDownloader = ApkDownloader(context),
    val installerHelper: PackageInstallerHelper = PackageInstallerHelper(context)
) {

    private val prefs = context.getSharedPreferences("mydoc_updater", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    val currentVersionName: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_NAME
        }

    fun cleanUpdatesCache() {
        val updatesDir = File(context.cacheDir, "updates")
        if (updatesDir.exists()) {
            updatesDir.listFiles()?.forEach { file ->
                runCatching { file.delete() }
            }
        }
    }

    suspend fun checkForUpdates(isLaunchCheck: Boolean = false) {
        // Debug builds use the debug signing key, so a release APK can never install
        // over them. Don't offer an update the device would reject.
        if (BuildConfig.DEBUG) {
            _state.value = UpdateUiState.Idle
            return
        }
        if (isLaunchCheck) {
            val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
            val now = System.currentTimeMillis()
            val twentyFourHours = 24 * 60 * 60 * 1000L
            if (now - lastCheckTime < twentyFourHours) {
                // Throttle launch checks to max once every 24 hours
                return
            }
        }

        _state.value = UpdateUiState.Checking

        val result = updateService.fetchLatestRelease()
        result.onSuccess { release ->
            // Only stamp the launch-check throttle on a successful fetch, so a transient failure
            // (offline, GitHub hiccup) does not suppress checks for the next 24 hours.
            if (isLaunchCheck) prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
            if (release == null) {
                _state.value = UpdateUiState.NoUpdate(currentVersionName, null)
                return
            }

            val ignoredTag = prefs.getString(KEY_IGNORED_TAG, "")
            if (isNewerVersion(release.versionName, currentVersionName)) {
                if (isLaunchCheck && ignoredTag == release.tagName) {
                    _state.value = UpdateUiState.Idle
                } else {
                    _state.value = UpdateUiState.UpdateAvailable(release, currentVersionName)
                }
            } else {
                _state.value = UpdateUiState.NoUpdate(currentVersionName, release)
            }
        }.onFailure { error ->
            if (!isLaunchCheck) {
                _state.value = UpdateUiState.Error(
                    error.localizedMessage ?: "Unable to check for updates. Please verify your connection."
                )
            } else {
                // Quietly ignore launch check failures
                _state.value = UpdateUiState.Idle
            }
        }
    }

    fun dismissUpdate(releaseTag: String? = null) {
        if (!releaseTag.isNullOrBlank()) {
            prefs.edit().putString(KEY_IGNORED_TAG, releaseTag).apply()
        }
        _state.value = UpdateUiState.Idle
    }

    suspend fun downloadAndInstall(release: AppRelease) {
        _state.value = UpdateUiState.Downloading(0, 0, release.apkSizeBytes)

        // Delete old APK files before attempt
        cleanUpdatesCache()

        val expectedChecksum = runCatching {
            if (release.checksumUrl.isNullOrBlank()) {
                throw IllegalStateException("Checksum URL is missing for release ${release.tagName}")
            }
            apkDownloader.fetchChecksumText(release.checksumUrl)
        }.getOrElse { ex ->
            cleanUpdatesCache()
            _state.value = UpdateUiState.Error("Could not retrieve the update checksum from GitHub. Try again or download the APK from GitHub Releases.")
            return
        }

        val targetName = "mydoc-v${release.versionName}.apk"
        val downloadResult = apkDownloader.downloadApk(
            downloadUrl = release.apkDownloadUrl,
            targetFileName = targetName,
            expectedSha256 = expectedChecksum,
            onProgress = { downloaded, total ->
                val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                _state.value = UpdateUiState.Downloading(percent, downloaded, total)
            }
        )

        downloadResult.onSuccess { file ->
            _state.value = UpdateUiState.ReadyToInstall(file, release)
            if (installerHelper.canInstallUnknownApps()) {
                runCatching {
                    installerHelper.installApk(file)
                }.onFailure { ex ->
                    cleanUpdatesCache()
                    _state.value = UpdateUiState.Error("The download could not be verified, so it was not installed.")
                }
            }
        }.onFailure { err ->
            cleanUpdatesCache()
            _state.value = UpdateUiState.Error("The download failed. Check your connection and try again.")
        }
    }

    fun install(apkFile: File) {
        runCatching { installerHelper.installApk(apkFile) }
            .onFailure { ex ->
                cleanUpdatesCache()
                _state.value = UpdateUiState.Error("The download could not be verified, so it was not installed.")
            }
    }

    fun isNewerVersion(remote: String, local: String): Boolean = Companion.isNewerVersion(remote, local)

    fun resetState() {
        _state.value = UpdateUiState.Idle
    }

    companion object {
        private const val KEY_LAST_CHECK_TIME = "last_update_check_time"
        private const val KEY_IGNORED_TAG = "last_ignored_release_tag"

        fun isNewerVersion(remote: String, local: String): Boolean {
            fun parts(value: String) = value
                .trim()
                .removePrefix("v")
                .substringBefore('-')
                .split('.')
                .mapNotNull { it.toIntOrNull() }

            val remoteParts = parts(remote)
            val localParts = parts(local)

            val maxLen = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }
    }
}
