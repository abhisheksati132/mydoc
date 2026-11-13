package com.abhisheksati.mydoc.update

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.abhisheksati.mydoc.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression coverage for the updater cache and state races fixed after the v2.0.5 audit:
 *  - S12: cache cleanup deleted the APK the system installer was still reading.
 *  - F40: dismiss/reset wrote Idle without invalidating an in-flight download's late progress writes.
 */
class UpdateManagerStateTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private class FakePrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()
        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            values[key] as? MutableSet<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class Editor : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?) = apply { values[key!!] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { this@FakePrefs.values[key!!] = values }
            override fun putInt(key: String?, value: Int) = apply { values[key!!] = value }
            override fun putLong(key: String?, value: Long) = apply { values[key!!] = value }
            override fun putFloat(key: String?, value: Float) = apply { values[key!!] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { values[key!!] = value }
            override fun remove(key: String?) = apply { values.remove(key) }
            override fun clear() = apply { values.clear() }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }

    private fun contextWith(prefs: SharedPreferences) = object : ContextWrapper(null) {
        override fun getCacheDir(): File = tempFolder.root
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private fun manager(): UpdateManager = UpdateManager(contextWith(FakePrefs()))

    private fun managerWith(
        prefs: SharedPreferences,
        downloader: ApkDownloader,
        installer: PackageInstallerHelper
    ): UpdateManager = UpdateManager(contextWith(prefs), apkDownloader = downloader, installerHelper = installer)

    private class FakeInstaller : PackageInstallerHelper(ContextWrapper(null)) {
        val installed = mutableListOf<File>()
        override fun canInstallUnknownApps(): Boolean = true
        override fun installApk(apkFile: File) {
            installed.add(apkFile)
        }
    }

    private class FakeDownloader(
        private val updatesDir: File,
        private val content: String = "NEW",
        private val started: CountDownLatch? = null,
        private val gate: CountDownLatch? = null
    ) : ApkDownloader(ContextWrapper(null)) {
        override suspend fun fetchChecksumText(checksumUrl: String): String = "0".repeat(64)
        override suspend fun downloadApk(
            downloadUrl: String,
            targetFileName: String,
            expectedSha256: String,
            onProgress: (Long, Long) -> Unit,
            accept: String?
        ): Result<File> {
            val target = File(updatesDir, targetFileName)
            target.parentFile?.mkdirs()
            target.writeText(content)
            started?.countDown()
            gate?.await(10, TimeUnit.SECONDS)
            return Result.success(target)
        }
    }

    private fun release(version: String = "9.9.9") = AppRelease(
        tagName = "v$version",
        versionName = version,
        releaseNotes = "",
        apkDownloadUrl = "https://github.com/example/app.apk",
        apkSizeBytes = 3,
        checksumUrl = "https://github.com/example/app.apk.sha256"
    )

    private fun setActiveApk(manager: UpdateManager, apk: File?) {
        val field = UpdateManager::class.java.getDeclaredField("activeInstallApk").apply { isAccessible = true }
        field.set(manager, apk)
    }

    @Test
    fun `cache cleanup keeps the apk an install may still be reading`() {
        val updates = File(tempFolder.root, "updates").apply { mkdirs() }
        val active = File(updates, "active.apk").apply { writeText("A") }
        val stale = File(updates, "stale.apk").apply { writeText("S") }
        val manager = manager()
        setActiveApk(manager, active)

        manager.cleanUpdatesCache()

        assertTrue("the active install APK must survive cleanup", active.exists())
        assertFalse("stale APKs are reclaimed", stale.exists())
    }

    @Test
    fun `cache cleanup with no active apk reclaims everything`() {
        val updates = File(tempFolder.root, "updates").apply { mkdirs() }
        val stale = File(updates, "stale.apk").apply { writeText("S") }
        val manager = manager()
        setActiveApk(manager, null)

        manager.cleanUpdatesCache()

        assertFalse(stale.exists())
    }

    @Test
    fun `a late progress write cannot repopulate state after dismiss`() {
        val manager = manager()
        val genField = UpdateManager::class.java.getDeclaredField("generation").apply { isAccessible = true }
        val generation = genField.get(manager) as java.util.concurrent.atomic.AtomicInteger
        val staleGen = generation.get()

        manager.dismissUpdate()
        assertEquals(UpdateUiState.Idle, manager.state.value)

        // Simulate the in-flight download's late onProgress using the generation it captured before dismiss.
        val setter = UpdateManager::class.java.getDeclaredMethod(
            "setStateIfCurrent", Int::class.javaPrimitiveType, UpdateUiState::class.java
        ).apply { isAccessible = true }
        setter.invoke(manager, staleGen, UpdateUiState.Downloading(50, 10, 20))

        assertEquals("a dismissed download must not write Downloading back over Idle", UpdateUiState.Idle, manager.state.value)
    }

    @Test
    fun `a current-generation write still lands`() {
        val manager = manager()
        val genField = UpdateManager::class.java.getDeclaredField("generation").apply { isAccessible = true }
        val generation = genField.get(manager) as java.util.concurrent.atomic.AtomicInteger
        val currentGen = generation.get()

        val setter = UpdateManager::class.java.getDeclaredMethod(
            "setStateIfCurrent", Int::class.javaPrimitiveType, UpdateUiState::class.java
        ).apply { isAccessible = true }
        setter.invoke(manager, currentGen, UpdateUiState.Downloading(50, 10, 20))

        assertTrue(manager.state.value is UpdateUiState.Downloading)
    }

    @Test(timeout = 30000)
    fun `dismiss during download prevents auto-install`() {
        val updates = File(tempFolder.root, "updates").apply { mkdirs() }
        val started = CountDownLatch(1)
        val gate = CountDownLatch(1)
        val installer = FakeInstaller()
        val manager = managerWith(FakePrefs(), FakeDownloader(updates, started = started, gate = gate), installer)

        runBlocking {
            val job = launch(Dispatchers.Default) { manager.downloadAndInstall(release()) }
            try {
                assertTrue("download never started", started.await(5, TimeUnit.SECONDS))
                manager.dismissUpdate()
            } finally {
                gate.countDown()
                job.join()
            }
        }

        assertTrue("a dismissed download must not launch the installer", installer.installed.isEmpty())
        assertEquals(UpdateUiState.Idle, manager.state.value)
    }

    @Test
    fun `active apk protection survives a process restart`() {
        val updates = File(tempFolder.root, "updates").apply { mkdirs() }
        val active = File(updates, "mydoc-v9.9.9.apk").apply { writeText("OLD") }
        val prefs = FakePrefs()
        val first = managerWith(prefs, FakeDownloader(updates), FakeInstaller())
        first.install(active)

        val second = managerWith(prefs, FakeDownloader(updates), FakeInstaller())
        second.cleanUpdatesCache()

        assertTrue("a restarted process must still protect the APK the installer may read", active.exists())
    }

    @Test
    fun `re-downloading a version never overwrites the apk an install may be reading`() {
        val updates = File(tempFolder.root, "updates").apply { mkdirs() }
        val active = File(updates, "mydoc-v9.9.9.apk").apply { writeText("OLD") }
        val installer = FakeInstaller()
        val manager = managerWith(FakePrefs(), FakeDownloader(updates, content = "NEW"), installer)
        manager.install(active)

        runBlocking { manager.downloadAndInstall(release()) }

        assertEquals("the active install APK was overwritten in place", "OLD", active.readText())
        assertEquals(2, installer.installed.size)
        assertFalse(installer.installed.last().absolutePath == active.absolutePath)
    }

    @Test(timeout = 30000)
    fun `cache cleanup keeps an in-flight download`() {
        val updates = File(tempFolder.root, "updates").apply { mkdirs() }
        val started = CountDownLatch(1)
        val gate = CountDownLatch(1)
        val manager = managerWith(FakePrefs(), FakeDownloader(updates, started = started, gate = gate), FakeInstaller())

        runBlocking {
            val job = launch(Dispatchers.Default) { manager.downloadAndInstall(release()) }
            try {
                assertTrue("download never started", started.await(5, TimeUnit.SECONDS))
                manager.cleanUpdatesCache()
                assertTrue("cleanup deleted the in-flight download", File(updates, "mydoc-v9.9.9.apk").exists())
            } finally {
                gate.countDown()
                job.join()
            }
        }
    }
}