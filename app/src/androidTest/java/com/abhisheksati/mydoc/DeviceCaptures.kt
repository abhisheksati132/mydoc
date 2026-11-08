package com.abhisheksati.mydoc

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Synthetic test UI only; captured artifacts make layout regressions reviewable in CI. */
object DeviceCaptures {
    fun save(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("ui-captures"))
        directory.mkdirs()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
}
