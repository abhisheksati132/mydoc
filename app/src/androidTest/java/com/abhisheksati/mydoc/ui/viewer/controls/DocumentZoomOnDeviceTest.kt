package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentZoomOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun zoomButtonDoesNotTurnPageOrHideControls() {
        val zoom = ZoomState()
        var turns = 0
        var toggles = 0
        compose.setContent {
            AppDesignSystemTheme {
                ZoomableViewport(
                    zoomState = zoom, isContinuous = false, isDualPage = false,
                    onNextPage = { turns++ }, onPrevPage = { turns++ },
                    onToggleControls = { toggles++ }, modifier = Modifier.size(320.dp)
                ) { Text("Document") }
            }
        }
        compose.onNodeWithContentDescription("Zoom, 100%").performClick()
        compose.onNodeWithText("300%").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(3f, zoom.scale, 0.01f)
            assertEquals(0, turns)
            assertEquals(0, toggles)
        }
        compose.onNodeWithText("Reset zoom").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1f, zoom.scale, 0.01f) }
    }

    @Test fun continuousDocumentScrollsAndPansWithoutLosingZoom() {
        val zoom = ZoomState(initialScale = 2f)
        lateinit var list: LazyListState
        compose.setContent {
            val state = rememberLazyListState()
            SideEffect { list = state }
            AppDesignSystemTheme {
                ZoomableViewport(
                    zoomState = zoom, isContinuous = true, isDualPage = false,
                    onVerticalScroll = { state.dispatchRawDelta(it) },
                    onNextPage = {}, onPrevPage = {}, onToggleControls = {},
                    showZoomControls = false,
                    modifier = Modifier.size(320.dp).testTag("viewport")
                ) {
                    LazyColumn(state = state) {
                        items(100) { index -> Box(Modifier.height(48.dp)) { Text("Page $index") } }
                    }
                }
            }
        }
        compose.onNodeWithTag("viewport").performTouchInput { swipeUp(durationMillis = 400) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(list.firstVisibleItemIndex > 0)
            assertEquals(2f, zoom.scale, 0.01f)
        }
        compose.onNodeWithTag("viewport").performTouchInput { swipeLeft(durationMillis = 400) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(zoom.offset.x < 0f)
            assertEquals(2f, zoom.scale, 0.01f)
        }
    }
}
