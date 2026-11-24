package com.abhisheksati.mydoc.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.abhisheksati.mydoc.DeviceCaptures
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.ui.ToolsScreen
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.ui.home.ContinueReadingHeroCard
import com.abhisheksati.mydoc.ui.home.HomeFeedPane
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerBottomBar
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerTopBar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FluidScreensOnDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val document = LibraryItem("file:///missing/Field-notes.pdf", "Field notes — a quieter workspace.pdf",
        MediaKind.DOCUMENT, "application/pdf", 185000, 1700000000000, lastPage = 3, pageCount = 24)

    @Test fun homeHasAnObviousResumeActionAndRealRecentFiles() {
        var opened: LibraryItem? = null
        compose.setContent { AppDesignSystemTheme {
            HomeFeedPane(listOf(document), listOf(document, document.copy(uri = "file:///missing/Brief.pdf", name = "Project brief.pdf")),
                onLibraryItemClick = { opened = it }, onLibraryItemDetails = {}, onRemoveFromRecents = {},
                onShareDocument = {}, selectedRecentUris = emptySet(), onToggleRecentSelection = {},
                onSelectAllRecents = {}, onClearRecentSelection = {}, onShareSelectedRecents = {},
                onRemoveSelectedRecents = {}, onSpotlightSearch = {}, onSettingsClick = {}, onScanClick = {},
                onNavigateToLibrary = {}, modifier = Modifier.background(AppTheme.colors.background))
        } }
        compose.onNodeWithText("Resume").assertIsDisplayed()
        DeviceCaptures.save("fluid-home")
        compose.onNodeWithContentDescription("Create or open").performClick()
        compose.onAllNodesWithText("Open document").assertCountEquals(2)
        DeviceCaptures.save("fluid-home-create")
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("Resume").performClick()
        compose.runOnIdle { assertEquals(document.uri, opened?.uri) }
    }

    @Test fun heroStacksAtDoubleSizeTextWithoutTruncatingTheResumeAction() {
        var opened = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AppDesignSystemTheme {
                    Column(Modifier.width(288.dp).verticalScroll(rememberScrollState())) {
                        ContinueReadingHeroCard(document) { opened = true }
                    }
                }
            }
        }
        compose.onNodeWithText("Resume").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(opened)
    }

    @Test fun homeWithLiveBackdropKeepsItsCreateMenuResponsive() {
        compose.setContent { AppDesignSystemTheme {
            val haze = remember { dev.chrisbanes.haze.HazeState() }
            CompositionLocalProvider(
                com.abhisheksati.mydoc.designsystem.components.LocalGlassEffectMode provides com.abhisheksati.mydoc.designsystem.components.GlassEffectMode.ON) {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                    HomeFeedPane(listOf(document), listOf(document), {}, {}, {}, {}, emptySet(), {}, {}, {}, {}, {},
                        {}, {}, {}, {}, hazeState = haze)
                    com.abhisheksati.mydoc.designsystem.components.BottomNavBar(
                        com.abhisheksati.mydoc.domain.model.HomeTab.HOME, {},
                        modifier = Modifier.align(Alignment.BottomCenter), hazeState = haze)
                }
            }
        } }
        compose.onNodeWithContentDescription("Create or open").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Open document").assertCountEquals(2)
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("Resume").assertIsDisplayed()
    }

    @Test fun featuredToolsStayActionableAfterTheVisualRefresh() {
        val model = ToolsViewModel(ApplicationProvider.getApplicationContext())
        var selected: PdfTool? = null
        compose.setContent { AppDesignSystemTheme {
            ToolsScreen(model, { selected = it }, modifier = Modifier.background(AppTheme.colors.background))
        } }
        compose.onNodeWithText("Scan to PDF").assertIsDisplayed()
        compose.waitForIdle()
        DeviceCaptures.save("fluid-tools", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.onNodeWithText("Scan to PDF").performClick()
        compose.runOnIdle { assertEquals(PdfTool.SCAN_TO_PDF, selected) }
    }

    @Test fun floatingReaderKeepsSearchBackAndOverflowActionsReachable() {
        var searched = false
        var backed = false
        var bookmarked = false
        compose.setContent { AppDesignSystemTheme {
            Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                ViewerTopBar(document.name, 3, 24, true, false,
                    { backed = true }, { searched = true }, { bookmarked = true }, {})
                ViewerBottomBar(3, true, true, {}, {}, {}, modifier = Modifier.align(Alignment.BottomCenter))
            }
        } }
        compose.onNodeWithContentDescription("Search text in document").assertIsDisplayed()
        compose.waitForIdle()
        DeviceCaptures.save("fluid-reader-controls", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.onNodeWithContentDescription("Search text in document").performClick()
        assertTrue(searched)
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Add page bookmark").performClick()
        assertTrue(bookmarked)
        compose.onNodeWithContentDescription("Navigate back").performClick()
        assertTrue(backed)
    }

    @Test fun readerActionsRemainReachableWithLargeTextAndRtl() {
        var appearance = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                AppDesignSystemTheme {
                    Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                        ViewerTopBar(document.name, 3, 24, true, false, {}, {}, {}, {})
                        ViewerBottomBar(3, true, true, {}, {}, { appearance = true },
                            modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Search text in document").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reading appearance settings").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(appearance)
        DeviceCaptures.save("fluid-reader-large-rtl")
    }
}
