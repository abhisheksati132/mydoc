package com.abhisheksati.mydoc

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

/** Synthetic test UI only; captured artifacts make layout regressions reviewable in CI. */
object DeviceCaptures {
    fun save(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                // Shared test media survives Gradle uninstalling the target APK after the suite.
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyDocTestCaptures")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                requireNotNull(resolver.openOutputStream(uri)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } else {
                val directory = requireNotNull(context.getExternalFilesDir("ui-captures"))
                directory.mkdirs()
                File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        } finally { bitmap.recycle() }
    }
}
