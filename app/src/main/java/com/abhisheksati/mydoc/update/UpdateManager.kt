package com.abhisheksati.mydoc.update

import android.content.Context
import android.content.pm.PackageManager
import com.abhisheksati.mydoc.BuildConfig
import com.abhisheksati.mydoc.domain.model.AppRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

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

    private val operation = kotlinx.coroutines.sync.Mutex()
    private val prefs = context.getSharedPreferences("mydoc_updater", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    // Bumped by dismiss/reset so an in-flight check or download can no longer repopulate the state
    // the user just cleared. Without it, a late onProgress wrote Downloading back over Idle.
    private val generation = AtomicInteger(0)

    // The check-then-write in setStateIfCurrent must be atomic against dismiss/reset, or a late
    // write can land between the generation check and the flow write and resurrect cleared state.
    private val stateLock = Any()

    // The APK the system installer may still be reading. Cache cleanup must never delete it out
    // from under an install; it is superseded only when a newer download completes. Persisted so
    // the protection survives process death while the installer UI is still open.
    @Volatile
    private var activeInstallApk: File? = null

    // A superseded APK stays protected for a grace period: the installer may still open it after
    // a newer download replaced the active pointer.
    @Volatile
    private var supersededApk: File? = null
    @Volatile
    private var supersededAt: Long = 0L

    // A download in progress; cleanup must not delete the partial file out from under it.
    @Volatile
    private var downloadingApk: File? = null

    init {
        activeInstallApk = prefs.getString(KEY_ACTIVE_APK, null)?.let { File(it) }?.takeIf { it.exists() }
        supersededApk = prefs.getString(KEY_SUPERSEDED_APK, null)?.let { File(it) }?.takeIf { it.exists() }
        supersededAt = prefs.getLong(KEY_SUPERSEDED_AT, 0L)
    }

    val currentVersionName: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: PackageManager.NameNotFoundException) {
            BuildConfig.VERSION_NAME
        }

    fun cleanUpdatesCache() {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) return
        val protectedPaths = setOfNotNull(
            activeInstallApk?.absolutePath,
            downloadingApk?.absolutePath,
            supersededApk?.takeIf { System.currentTimeMillis() - supersededAt < SUPERSEDE_GRACE_MS }?.absolutePath
        )
        updatesDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in protectedPaths) {
                runCatching { file.delete() }
            }
        }
    }

    private fun setStateIfCurrent(gen: Int, state: UpdateUiState) {
        synchronized(stateLock) {
            if (generation.get() == gen) _state.value = state
        }
    }

    private fun setActiveApk(file: File) {
        val previous = activeInstallApk
        if (previous != null && previous.absolutePath != file.absolutePath) {
            supersededApk = previous
            supersededAt = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_SUPERSEDED_APK, previous.absolutePath)
                .putLong(KEY_SUPERSEDED_AT, supersededAt)
                .apply()
        }
        activeInstallApk = file
        prefs.edit().putString(KEY_ACTIVE_APK, file.absolutePath).apply()
    }

    suspend fun checkForUpdates(isLaunchCheck: Boolean = false) {
        if (!operation.tryLock()) {
            if (!isLaunchCheck) {
                val gen = generation.get()
                synchronized(stateLock) {
                    if (_state.value is UpdateUiState.Idle) setStateIfCurrent(gen, UpdateUiState.Checking)
                }
            }
            return
        }
        val gen = generation.get()
        try {
            // Debug builds use the debug signing key, so a release APK can never install
            // over them. Don't offer an update the device would reject.
            if (BuildConfig.DEBUG) {
                setStateIfCurrent(gen, UpdateUiState.Idle)
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

            setStateIfCurrent(gen, UpdateUiState.Checking)

            val result = updateService.fetchLatestRelease()
            result.onSuccess { release ->
                // Only stamp the launch-check throttle on a successful fetch, so a transient failure
                // (offline, GitHub hiccup) does not suppress checks for the next 24 hours.
                if (isLaunchCheck) prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
                if (release == null) {
                    setStateIfCurrent(gen, UpdateUiState.NoUpdate(currentVersionName, null))
                    return
                }

                val ignoredTag = prefs.getString(KEY_IGNORED_TAG, "")
                if (isNewerVersion(release.versionName, currentVersionName)) {
                    if (isLaunchCheck && ignoredTag == release.tagName) {
                        setStateIfCurrent(gen, UpdateUiState.Idle)
                    } else {
                        setStateIfCurrent(gen, UpdateUiState.UpdateAvailable(release, currentVersionName))
                    }
                } else {
                    setStateIfCurrent(gen, UpdateUiState.NoUpdate(currentVersionName, release))
                }
            }.onFailure { error ->
                if (!isLaunchCheck) {
                    setStateIfCurrent(
                        gen,
                        UpdateUiState.Error(
                            error.localizedMessage ?: "Unable to check for updates. Please verify your connection."
                        )
                    )
                } else {
                    // Quietly ignore launch check failures
                    setStateIfCurrent(gen, UpdateUiState.Idle)
                }
            }
        } finally {
            operation.unlock()
        }
    }

    fun dismissUpdate(releaseTag: String? = null) {
        if (!releaseTag.isNullOrBlank()) {
            prefs.edit().putString(KEY_IGNORED_TAG, releaseTag).apply()
        }
        synchronized(stateLock) {
            generation.incrementAndGet()
            _state.value = UpdateUiState.Idle
        }
    }

    suspend fun downloadAndInstall(release: AppRelease) {
        if (!operation.tryLock()) return
        val gen = generation.get()
        try {
            setStateIfCurrent(gen, UpdateUiState.Downloading(0, 0, release.apkSizeBytes))

            // Remove stale APKs, but never the one an install may still be reading.
            cleanUpdatesCache()

            var targetName = "mydoc-v${release.versionName}.apk"
            val updatesDir = File(context.cacheDir, "updates")
            if (File(updatesDir, targetName).absolutePath == activeInstallApk?.absolutePath) {
                // Re-downloading the same version must not truncate the file an install may read.
                targetName = "mydoc-v${release.versionName}-${System.currentTimeMillis()}.apk"
            }
            downloadingApk = File(updatesDir, targetName)
            val onProgress: (Long, Long) -> Unit = { downloaded, total ->
                val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                setStateIfCurrent(gen, UpdateUiState.Downloading(percent, downloaded, total))
            }
            val downloadResult = runCatching {
                val checksum = apkDownloader.fetchChecksumText(checkNotNull(release.checksumUrl))
                apkDownloader.downloadApk(release.apkDownloadUrl, targetName, checksum, onProgress).getOrThrow()
            }.recoverCatching { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                val apiUrl = release.assetApiUrl ?: throw error
                val digest = release.assetSha256 ?: throw error
                apkDownloader.downloadApk(apiUrl, targetName, digest, onProgress,
                    accept = "application/octet-stream").getOrThrow()
            }
            downloadResult.onSuccess { file ->
                setActiveApk(file)
                setStateIfCurrent(gen, UpdateUiState.ReadyToInstall(file, release))
                // The new APK supersedes the previous one; reclaim the older files now.
                cleanUpdatesCache()
                val stillCurrent = synchronized(stateLock) { generation.get() == gen }
                if (stillCurrent && installerHelper.canInstallUnknownApps()) {
                    runCatching {
                        installerHelper.installApk(file)
                    }.onFailure {
                        setStateIfCurrent(gen, UpdateUiState.Error("The download could not be verified, so it was not installed."))
                    }
                }
            }.onFailure { err ->
                if (err is kotlinx.coroutines.CancellationException) throw err
                cleanUpdatesCache()
                setStateIfCurrent(gen, UpdateUiState.Error("The download failed. Check your connection and try again."))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            setStateIfCurrent(gen, UpdateUiState.Idle)
            throw e
        } finally {
            downloadingApk = null
            operation.unlock()
        }
    }

    fun install(apkFile: File) {
        setActiveApk(apkFile)
        val gen = generation.get()
        runCatching { installerHelper.installApk(apkFile) }
            .onFailure {
                setStateIfCurrent(gen, UpdateUiState.Error("The download could not be verified, so it was not installed."))
            }
    }

    fun isNewerVersion(remote: String, local: String): Boolean = Companion.isNewerVersion(remote, local)

    fun resetState() {
        synchronized(stateLock) {
            generation.incrementAndGet()
            _state.value = UpdateUiState.Idle
        }
    }

    companion object {
        private const val KEY_LAST_CHECK_TIME = "last_update_check_time"
        private const val KEY_IGNORED_TAG = "last_ignored_release_tag"
        private const val KEY_ACTIVE_APK = "active_install_apk_path"
        private const val KEY_SUPERSEDED_APK = "superseded_install_apk_path"
        private const val KEY_SUPERSEDED_AT = "superseded_install_apk_at"
        private const val SUPERSEDE_GRACE_MS = 10 * 60 * 1000L

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
