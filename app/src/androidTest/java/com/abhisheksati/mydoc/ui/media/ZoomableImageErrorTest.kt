package com.abhisheksati.mydoc.ui.media

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.MediaItem
import org.junit.Rule
import org.junit.Test

class ZoomableImageErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unloadableImage_showsErrorPlaceholder() {
        val missing = MediaItem(
            id = 1L,
            uri = Uri.parse("content://com.abhisheksati.mydoc.missing/1"),
            displayName = "missing.jpg",
            sizeBytes = 0L,
            mimeType = "image/jpeg",
            dateAddedSec = 0L
        )
        compose.setContent {
            AppDesignSystemTheme {
                ZoomableImage(item = missing, isFillScale = false, displayRotation = 0f, onTap = {})
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("This image could not be loaded.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("This image could not be loaded.").assertExists()
    }
}