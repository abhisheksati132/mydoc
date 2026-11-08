package com.abhisheksati.mydoc.util

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LocalApkInstallerOnDeviceTest {
    @Test fun stagedPackagesHaveDistinctReadableUrisAndInstallerMime() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File.createTempFile("installer-source-", ".apk", context.cacheDir)
        try {
            val bytes = "test installer payload".toByteArray()
            source.writeBytes(bytes)
            val first = LocalApkInstaller.stage(context, Uri.fromFile(source))
            val second = LocalApkInstaller.stage(context, Uri.fromFile(source))
            assertNotEquals(first, second)
            assertEquals("content", first.scheme)
            context.contentResolver.openInputStream(first)!!.use { assertArrayEquals(bytes, it.readBytes()) }
            val intent = LocalApkInstaller.intentFor(first)
            assertEquals(LocalApkInstaller.MIME, intent.type)
            assertEquals(first, intent.data)
            assertEquals(first, intent.clipData!!.getItemAt(0).uri)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertNotNull("Android must offer a package installer", intent.resolveActivity(context.packageManager))
        } finally { source.delete() }
    }

    @Test fun engineCleanupPreservesOtherStagedInstallersAndOriginalFiles() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File.createTempFile("installer-cleanup-", ".apk", context.cacheDir)
        try {
            source.writeText("test package")
            val ordinary = LocalApkInstaller.stage(context, Uri.fromFile(source))
            val engine = LocalApkInstaller.stage(context, Uri.fromFile(source), prefix = "office-engine")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { LocalApkInstaller.clearOfficeEngineStaging(context) }
            assertTrue(source.isFile)
            context.contentResolver.openInputStream(ordinary)!!.use { assertEquals("test package", it.reader().readText()) }
            assertThrows(java.io.FileNotFoundException::class.java) { context.contentResolver.openInputStream(engine) }
        } finally { source.delete() }
    }
}
