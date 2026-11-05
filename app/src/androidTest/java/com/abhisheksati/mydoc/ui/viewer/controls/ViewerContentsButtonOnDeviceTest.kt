package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * N-4: the bottom bar was given `hasOutline` — the PDF viewer passes
 * `capabilities.supportsOutline && sheetItems.isNotEmpty()` — and then rendered the Contents button
 * with `enabled = true` regardless. A text file or an outline-less PDF therefore opened a sheet that
 * had nothing in it. The flag is honoured now, which is what this pins.
 */
@RunWith(AndroidJUnit4::class)
class ViewerContentsButtonOnDeviceTest {

    @get:Rule
    val compose = createComposeRule()

    private fun renderBottomBar(hasOutline: Boolean) {
        compose.setContent {
            AppDesignSystemTheme {
                ViewerBottomBar(
                    currentPage = 0,
                    pageCount = 5,
                    visible = true,
                    hasOutline = hasOutline,
                    onOutlineClick = {},
                    onPagesClick = {},
                    onAppearanceClick = {},
                    onPageScrubbed = {}
                )
            }
        }
    }

    @Test
    fun contentsIsNotOfferedWhenTheDocumentHasNoOutline() {
        renderBottomBar(hasOutline = false)
        compose.onNodeWithContentDescription("Document contents and outline").assertIsNotEnabled()
    }

    @Test
    fun contentsIsOfferedWhenAnOutlineExists() {
        renderBottomBar(hasOutline = true)
        compose.onNodeWithContentDescription("Document contents and outline").assertIsEnabled()
    }

    @Test
    fun theOtherControlsStayUsableEitherWay() {
        // The regression this guards against is gating the wrong button: pages and appearance have
        // nothing to do with the outline and must not inherit the flag.
        renderBottomBar(hasOutline = false)
        compose.onNodeWithContentDescription("Thumbnail page grid").assertIsEnabled()
        compose.onNodeWithContentDescription("Reading appearance settings").assertIsEnabled()
    }
}
