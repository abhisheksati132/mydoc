package com.abhisheksati.mydoc.rendering.engine

import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.domain.model.LoadFailure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class OptionalOfficeEngineOnDeviceTest {
    @Test fun unavailableEngineCannotServeCachedPagesAndWorksAgainWhenEnabled() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val engine = OfficeEngineManager.packageName(context)
        assertTrue("CI must install the matching signed companion", OfficeEngineManager.isInstalled(context))
        val source = File.createTempFile("optional-office-", ".docx", context.cacheDir)
        try {
            instrumentation.context.assets.open("word/fidelity.docx").use { input -> source.outputStream().use { input.copyTo(it) } }
            assertTrue(NativeOfficeConverter.convert(context, Uri.fromFile(source), source.name) is OfficeConversion.Success)
            shell("pm disable-user --user 0 $engine")
            assertFalse(OfficeEngineManager.isInstalled(context))
            val absent = NativeOfficeConverter.convert(context, Uri.fromFile(source), source.name)
            assertTrue(absent is OfficeConversion.Failure)
            assertEquals(LoadFailure.UNSUPPORTED_FORMAT, (absent as OfficeConversion.Failure).failure)
        } finally {
            shell("pm enable --user 0 $engine")
            source.delete()
        }
        assertTrue(OfficeEngineManager.isInstalled(context))
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
    }
}
