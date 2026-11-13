package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.StorageItem
import java.io.File
import org.junit.Rule
import org.junit.Test

class GalleryCellAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private val photo = StorageItem(
        File("/sdcard/Pictures/IMG_20260101_sunset.jpg"),
        sizeBytes = 2048L,
        lastModified = 1700000000000L
    )

    @Test
    fun galleryTile_announcesFileName() {
        compose.setContent {
            AppDesignSystemTheme {
                FileGridCell(
                    item = photo,
                    isSelected = false,
                    isSelectionMode = false,
                    onClick = {},
                    onLongClick = {},
                    onMoreClick = {},
                    galleryTile = true
                )
            }
        }
        compose.onNodeWithContentDescription(photo.name)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun regularGridTile_keepsVisibleName() {
        compose.setContent {
            AppDesignSystemTheme {
                FileGridCell(
                    item = photo,
                    isSelected = false,
                    isSelectionMode = false,
                    onClick = {},
                    onLongClick = {},
                    onMoreClick = {}
                )
            }
        }
        compose.onNodeWithText(photo.name).assertExists()
    }
}