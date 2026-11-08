package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BrowseExperienceOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingDoesNotOfferPreviousFolderFilesOrClaimTheFolderIsEmpty() {
        val old = StorageItem(File("/sdcard/old/stale.txt"))
        compose.setContent {
            AppDesignSystemTheme {
                DirectoryPane(FileManagerUiState(File("/sdcard/new"), items = listOf(old),
                    filteredItems = listOf(old), isLoading = true), {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("stale.txt").assertDoesNotExist()
        compose.onNodeWithText("Loading files…").assertIsDisplayed()
    }

    @Test fun sameFolderRefreshKeepsItsExistingRowsVisible() {
        val item = StorageItem(File("/sdcard/same/kept.txt"))
        compose.setContent {
            AppDesignSystemTheme {
                DirectoryPane(FileManagerUiState(File("/sdcard/same"), items = listOf(item),
                    filteredItems = listOf(item), isLoading = true, isRefreshing = true), {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("kept.txt").assertIsDisplayed()
        compose.onNodeWithText("Loading files…").assertDoesNotExist()
    }

    @Test fun emptySearchOffersAnImmediateRecoveryAction() {
        var cleared = false
        compose.setContent {
            AppDesignSystemTheme {
                DirectoryPane(FileManagerUiState(File("/sdcard"), searchQuery = "missing"),
                    {}, {}, {}, {}, {}, {}, onClearFilters = { cleared = true })
            }
        }
        compose.onNodeWithText("Clear search and filters").performScrollTo().performClick()
        assertTrue(cleared)
    }

    @Test fun emptySearchRecoveryRemainsReachableOnAShortScreen() {
        var cleared = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AppDesignSystemTheme {
                    Box(Modifier.width(320.dp).height(240.dp)) {
                        DirectoryPane(FileManagerUiState(File("/sdcard"), searchQuery = "missing"),
                            {}, {}, {}, {}, {}, {}, onClearFilters = { cleared = true })
                    }
                }
            }
        }
        compose.onNodeWithText("Clear search and filters").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(cleared)
    }

    @Test fun largeTextGridGivesFilesTheirOwnRowOnANarrowScreen() {
        val items = listOf(StorageItem(File("/sdcard/first.pdf")), StorageItem(File("/sdcard/second.pdf")))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AppDesignSystemTheme {
                    Box(Modifier.width(320.dp)) {
                        DirectoryPane(FileManagerUiState(File("/sdcard"), items = items,
                            filteredItems = items, isGridView = true), {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        val first = compose.onNodeWithText("first.pdf").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("second.pdf").fetchSemanticsNode().boundsInRoot
        assertTrue("Large-text grids should add rows instead of squeezing labels into columns", second.top > first.bottom)
    }

    @Test fun fileMetadataIsReadableWithLargeText() {
        val item = StorageItem(File("/sdcard/Quarterly financial report.pdf"), sizeBytes = 123456L, lastModified = 1700000000000L)
        val metadata = "${item.formattedSize} · ${item.formattedLastModified}"
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AppDesignSystemTheme {
                    Box(Modifier.width(320.dp)) { FileRow(item, false, false, {}, {}, {}) }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(metadata, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse("Essential file metadata must wrap rather than disappear", layouts.single().hasVisualOverflow)
    }
}
