package com.abhisheksati.mydoc.tools

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.DeviceCaptures
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.ui.components.ImagePdfEditorSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImagePdfEditorOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    private fun renderEditor(invalidImage: Boolean = false, onCreate: (List<Uri>) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "editor-layout-fixture.png")
        if (invalidImage) {
            source.writeText("invalid image stream")
        } else {
            val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.rgb(235, 227, 209))
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        compose.setContent {
            var options by remember { mutableStateOf(ToolOptions()) }
            AppDesignSystemTheme {
                ImagePdfEditorSheet(
                    initialUris = listOf(Uri.fromFile(source)), options = options,
                    onOptionsChange = { options = it(options) }, onDismiss = {},
                    onCreate = onCreate
                )
            }
        }
        compose.waitUntil(10_000) {
            val matcher = if (invalidImage) androidx.compose.ui.test.hasText("None of the selected images could be read.")
                else androidx.compose.ui.test.hasContentDescription("Page 1 of 1")
            compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    @Test fun previewSettingsAndCreateRemainAccessible() {
        var created: List<Uri>? = null
        renderEditor { created = it }
        compose.onNodeWithText("Create PDF").assertIsDisplayed()
        DeviceCaptures.save("image-pdf-editor-layout")
        val settings = compose.onNodeWithText("Page & PDF settings")
        settings.performScrollTo()
        settings.performClick()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Create PDF").performClick()
        compose.waitUntil(10_000) { created != null }
        assertEquals(1, created!!.size)
    }
    @Test fun failedCreateUnlocksTheEditorForRetry() {
        var attempts = 0
        renderEditor {
            attempts++
            throw IllegalStateException("Synthetic delivery failure")
        }
        compose.onNodeWithText("Create PDF").performClick()
        compose.waitUntil(10_000) { attempts == 1 }
        compose.waitForIdle()
        compose.onNodeWithText("Could not prepare the PDF. Try fewer or smaller images.").assertIsDisplayed()
        compose.onNodeWithText("Create PDF").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { attempts == 2 }
    }

    @Test fun adjustmentsCanBeComparedAndResetBeforeExport() {
        renderEditor { }
        compose.onNodeWithContentDescription("Rotate right").performScrollTo().performClick()
        compose.onNodeWithText("Before adjustments").assertIsDisplayed().performClick()
        compose.onNodeWithText("Show edits").assertIsDisplayed().performClick()
        compose.onNodeWithText("Before adjustments").assertIsDisplayed()
        compose.waitForIdle()
        DeviceCaptures.save("fluid-image-pdf-comparison")
        compose.onNodeWithText("Reset rotation and filter").performScrollTo().performClick()
        compose.onNodeWithText("Before adjustments").assertDoesNotExist()
        compose.onNodeWithText("Create PDF").assertIsEnabled()
    }

    @Test fun malformedImageIsRejectedWithoutAnEndlessPreviewSpinner() {
        var exports = 0
        renderEditor(invalidImage = true) { exports++ }
        compose.onNodeWithText("Create PDF").assertDoesNotExist()
        assertEquals(0, exports)
    }

    @Test fun cropIsCommittedBeforeCreateCanExport() {
        var created: List<Uri>? = null
        renderEditor { created = it }
        compose.onNodeWithContentDescription("Crop").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Apply Crop")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Apply Crop").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("Create PDF") and androidx.compose.ui.test.isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Create PDF").performClick()
        compose.waitUntil(10_000) { created != null }
        org.junit.Assert.assertTrue(created!!.single().toString().contains("crop_"))
    }

}
