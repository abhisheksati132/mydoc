package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import java.io.File
import org.junit.Rule
import org.junit.Test

class DirectoryScrollRestorationOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun listRestoresItsPositionButNewFoldersStartAtTop() = verifyScroll(false)
    @Test fun gridRestoresItsPositionButNewFoldersStartAtTop() = verifyScroll(true)

    private fun verifyScroll(grid: Boolean) {
        val items = (0..79).map { StorageItem(File("/sdcard/first/item%03d.txt".format(it))) }
        val state = mutableStateOf(FileManagerUiState(File("/sdcard/first"), items = items,
            filteredItems = items, isGridView = grid))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            AppDesignSystemTheme {
                DirectoryPane(state.value, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("item060.txt"))
        compose.onNodeWithText("item060.txt").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("item060.txt").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(currentDirectory = File("/sdcard/second")) }
        compose.onNodeWithText("item000.txt").assertIsDisplayed()
    }
}
