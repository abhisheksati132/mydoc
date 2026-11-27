package com.abhisheksati.mydoc.ui.files

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class ArchiveEngineOnDeviceTest {
    @Test fun installedCurrentEngineRunsInsideTheAppSandbox() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = File(context.applicationInfo.nativeLibraryDir, "libmydoc7z.so")
        assertTrue("Archive engine must be packaged for the current ABI", engine.isFile)
        val pidFile = File(context.cacheDir, "engine-probe-${System.nanoTime()}.pid")
        try {
            val process = ProcessBuilder(engine.absolutePath, "i").redirectErrorStream(true).apply {
                environment()["MYDOC_ARCHIVE_PID_FILE"] = pidFile.absolutePath
            }.start()
            val output = process.inputStream.bufferedReader().readText()
            assertTrue(process.waitFor(15, TimeUnit.SECONDS))
            assertEquals(output, 0, process.exitValue())
            assertTrue(output.contains("26.06"))
            assertTrue(output.contains("7z"))
            assertTrue(pidFile.readText().toInt() > 0)
        } finally { pidFile.delete() }
    }
}
