package com.abhisheksati.mydoc.testutil

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Apply before launching the activity: modal windows read density from Android. */
class AndroidFontScaleRule(private val scale: Float = 2f, private val onlyTestName: String? = null) : TestWatcher() {
    private var original: Float? = null

    override fun starting(description: Description) {
        if (onlyTestName != null && description.methodName != onlyTestName) return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        original = Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE, 1f)
        setScale(scale)
    }

    override fun finished(description: Description) {
        original?.let { setScale(it) }
        original = null
    }

    private fun setScale(value: Float) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "settings put system font_scale $value")).use { it.readBytes() }
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (instrumentation.targetContext.resources.configuration.fontScale != value && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50)
        }
        assertEquals(value, instrumentation.targetContext.resources.configuration.fontScale, 0.01f)
    }
}
