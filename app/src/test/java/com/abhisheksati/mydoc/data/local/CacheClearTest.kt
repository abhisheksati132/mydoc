package com.abhisheksati.mydoc.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The Settings cache size "never reached zero": the pending update APK lives in cacheDir/updates
 * and survived the clear but was still counted. These tests pin both halves of the fix —
 * measurement ignores the updates directory, and clearing deletes everything else.
 */
class CacheClearTest {

    private fun tempDir(prefix: String): File =
        Files.createTempDirectory(prefix).toFile().also { it.deleteOnExit() }

    private fun fileIn(dir: File, name: String, bytes: Int): File {
        val target = File(dir, name)
        target.parentFile?.mkdirs()
        target.writeBytes(ByteArray(bytes))
        return target
    }

    private fun sampleCache(): Triple<File, File, File> {
        val internal = tempDir("cache-internal")
        val external = tempDir("cache-external")
        val updates = tempDir("cache-updates")
        fileIn(internal, "thumbnail.png", 10)
        fileIn(File(internal, "renders"), "page.bin", 20)
        fileIn(updates, "pending.apk", 30)
        fileIn(external, "external_thumb.bin", 5)
        return Triple(internal, external, updates)
    }

    private fun cacheWithUpdates(internal: File, updates: File): File {
        val moved = File(internal, "updates")
        check(updates.renameTo(moved)) { "could not move the updates dir into the cache" }
        return moved
    }

    @Test
    fun cacheSize_ignoresThePendingUpdateDirectory() {
        val (internal, external, updates) = sampleCache()
        val updatesInCache = cacheWithUpdates(internal, updates)

        // 10 + 20 internal + 5 external; the 30-byte pending APK must not be measured.
        assertEquals(35L, SettingsRepository.clearableCacheBytes(internal, listOf(external)))
        assertEquals(30L, SettingsRepository.folderSize(updatesInCache))
    }

    @Test
    fun clear_deletesEverythingButThePendingUpdate_andReadsZeroAfterwards() {
        val (internal, external, updates) = sampleCache()
        cacheWithUpdates(internal, updates)

        SettingsRepository.purgeCacheDirectories(internal, listOf(external))

        assertFalse(File(internal, "thumbnail.png").exists())
        assertFalse(File(internal, "renders").exists())
        assertFalse(File(external, "external_thumb.bin").exists())
        assertTrue("the pending update must survive a cache clear", File(internal, "updates/pending.apk").exists())
        assertEquals(0L, SettingsRepository.clearableCacheBytes(internal, listOf(external)))
    }

    @Test
    fun missingDirectories_readAsZeroAndClearWithoutError() {
        val absent = File(tempDir("cache-absent"), "never-created")
        assertEquals(0L, SettingsRepository.folderSize(null))
        assertEquals(0L, SettingsRepository.folderSize(absent))
        SettingsRepository.purgeCacheDirectories(null, listOf(absent))
        assertEquals(0L, SettingsRepository.clearableCacheBytes(null, listOf(absent)))
    }
}
