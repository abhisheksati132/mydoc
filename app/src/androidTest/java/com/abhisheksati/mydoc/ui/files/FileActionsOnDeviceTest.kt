package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.StorageItem
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FileActionsOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val file = StorageItem(File("/sdcard/report.pdf"))

    @Test fun gridActionsDoNotOpenTheFile() {
        var opened = false
        var more = false
        compose.setContent { AppDesignSystemTheme {
            Box(Modifier.width(120.dp)) {
                FileGridCell(file, false, false, { opened = true }, {}, { more = true })
            }
        } }
        compose.onNodeWithContentDescription("More options for report.pdf").performClick()
        assertTrue(more)
        assertFalse(opened)
    }

    @Test fun selectionGridKeepsItsCheckboxAndHidesTheOverflow() {
        var toggled = false
        compose.setContent { AppDesignSystemTheme {
            Box(Modifier.width(120.dp)) {
                FileGridCell(file, true, true, { toggled = true }, {}, {})
            }
        } }
        compose.onNodeWithContentDescription("More options for report.pdf").assertDoesNotExist()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        assertTrue(toggled)
    }

    @Test fun commonSelectionActionsFitWithLargeTextAndRemainDirect() {
        var copies = 0
        var moves = 0
        var shares = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AppDesignSystemTheme {
                    Box(Modifier.width(288.dp)) {
                        FilesSelectionBar(3, 10, {}, {}, { shares++ }, { copies++ }, { moves++ }, null, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Copy").assertIsDisplayed().performClick()
        compose.onNodeWithText("Move").assertIsDisplayed().performClick()
        compose.onNodeWithText("Share").assertIsDisplayed().performClick()
        assertEquals(1, copies)
        assertEquals(1, moves)
        assertEquals(1, shares)
    }

    @Test fun renameRemainsAvailableInTheSelectionOverflow() {
        var renamed = false
        compose.setContent { AppDesignSystemTheme {
            FilesSelectionBar(1, 3, {}, {}, {}, {}, {}, { renamed = true }, {}, {})
        } }
        compose.onNodeWithContentDescription("More actions").performClick()
        compose.onNodeWithText("Rename").performClick()
        assertTrue(renamed)
    }
}
