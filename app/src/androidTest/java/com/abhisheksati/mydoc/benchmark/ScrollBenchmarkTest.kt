package com.abhisheksati.mydoc.benchmark

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.ui.home.HomeFeedPane
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerBottomBar
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerTopBar
import kotlinx.coroutines.flow.distinctUntilChanged
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Scroll and frame timing benchmark for "Paper under glass".
 *
 * Measures:
 * 1. Home feed scroll performance: Hero paper card + 40-item recent grid + glass bottom bar.
 * 2. PDF viewer continuous scroll: Page scroll and condensing glass toolbar transitions.
 */
class ScrollBenchmarkTest {

    @get:Rule
    val compose = createComposeRule()

    private val tag = "ScrollBenchmark"

    @Test
    fun homeScreenScrollPerformance_measuresFrameAndScrollTimings() {
        val continueItems = listOf(
            LibraryItem(
                uri = "content://docs/hero-1",
                name = "Deep Learning Architecture 2026.pdf",
                kind = MediaKind.DOCUMENT,
                mimeType = "application/pdf",
                sizeBytes = 4_200_000L,
                lastModified = System.currentTimeMillis() - 1_200_000L,
                progressFraction = 0.65f,
                lastPage = 18,
                pageCount = 28
            )
        )

        val recentItems = (1..40).map { i ->
            LibraryItem(
                uri = "content://docs/recents/$i",
                name = "Research Paper $i.pdf",
                kind = MediaKind.DOCUMENT,
                mimeType = "application/pdf",
                sizeBytes = 1_200_000L + (i * 15_000L),
                lastModified = System.currentTimeMillis() - (i * 3600_000L)
            )
        }

        compose.setContent {
            AppDesignSystemTheme(darkTheme = true) {
                HomeFeedPane(
                    continueItems = continueItems,
                    recentItems = recentItems,
                    onLibraryItemClick = {},
                    onLibraryItemDetails = {},
                    onRemoveFromRecents = {},
                    onShareDocument = {},
                    selectedRecentUris = emptySet(),
                    onToggleRecentSelection = {},
                    onSelectAllRecents = {},
                    onClearRecentSelection = {},
                    onShareSelectedRecents = {},
                    onRemoveSelectedRecents = {},
                    onSpotlightSearch = {},
                    onSettingsClick = {},
                    onScanClick = {},
                    onNavigateToLibrary = {}
                )
            }
        }

        // Settle composition
        compose.waitForIdle()
        compose.onNodeWithText("Deep Learning Architecture 2026.pdf").assertIsDisplayed()

        // Measure scroll cycle duration across multiple gestures
        val gestureDurationsMs = mutableListOf<Long>()
        val scrollCycles = 4

        for (cycle in 1..scrollCycles) {
            val startTime = SystemClock.elapsedRealtime()
            compose.onRoot().performTouchInput {
                swipeUp(durationMillis = 200)
            }
            compose.waitForIdle()
            val duration = SystemClock.elapsedRealtime() - startTime
            gestureDurationsMs.add(duration)
        }

        // Scroll back down
        for (cycle in 1..scrollCycles) {
            val startTime = SystemClock.elapsedRealtime()
            compose.onRoot().performTouchInput {
                swipeDown(durationMillis = 200)
            }
            compose.waitForIdle()
            val duration = SystemClock.elapsedRealtime() - startTime
            gestureDurationsMs.add(duration)
        }

        val avgDurationMs = gestureDurationsMs.average()
        val maxDurationMs = gestureDurationsMs.maxOrNull() ?: 0L

        Log.i(tag, "Home scroll benchmark: cycles=$scrollCycles avgGestureMs=$avgDurationMs maxGestureMs=$maxDurationMs")

        // Assert smooth gesture processing (each 200ms swipe completes promptly without ANR/hang)
        assertTrue("Average swipe gesture took too long: $avgDurationMs ms", avgDurationMs < 2500.0)
    }

    @Test
    fun pdfViewerScrollPerformance_toolbarCondensesAndExpandsSmoothly() {
        var isCondensedState by mutableStateOf(false)

        compose.setContent {
            AppDesignSystemTheme(darkTheme = true) {
                PdfViewerBenchmarkHost(
                    isCondensed = isCondensedState,
                    onCondensedChanged = { isCondensedState = it }
                )
            }
        }

        compose.waitForIdle()
        // Top bar expanded initially: title is visible, middle-truncated to fit
        val expandedTitle = formatMiddleTruncated("Quarterly Financial Analysis 2026.pdf", maxChars = 28)
        compose.onNodeWithText(expandedTitle).assertIsDisplayed()

        val timings = mutableListOf<Long>()

        // Fling scroll down: triggers condensed floating glass pill
        val t0 = SystemClock.elapsedRealtime()
        compose.onNodeWithTag("pdf_scroll_pages").performTouchInput {
            swipeUp(durationMillis = 200)
        }
        compose.waitForIdle()
        timings.add(SystemClock.elapsedRealtime() - t0)

        // Verify condensed state triggered
        assertTrue("Toolbar did not condense upon scroll", isCondensedState)
        // Condensed pill displays monospace page indicator
        compose.onNodeWithText("5/40").assertIsDisplayed()

        // Fling scroll back to top: expands back to full Fraunces masthead
        val t1 = SystemClock.elapsedRealtime()
        compose.onNodeWithTag("pdf_scroll_pages").performTouchInput {
            swipeDown(durationMillis = 200)
        }
        compose.waitForIdle()
        timings.add(SystemClock.elapsedRealtime() - t1)

        val avgTransitionMs = timings.average()
        Log.i(tag, "PDF viewer condensing scroll benchmark: avgTransitionMs=$avgTransitionMs timings=$timings")

        assertTrue("Scroll transition took too long: $avgTransitionMs ms", avgTransitionMs < 2500.0)
    }
}

@Composable
private fun PdfViewerBenchmarkHost(
    isCondensed: Boolean,
    onCondensedChanged: (Boolean) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 }
            .distinctUntilChanged()
            .collect { condensed ->
                onCondensedChanged(condensed)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("pdf_scroll_pages")
        ) {
            items(40) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(16.dp)
                        .background(AppTheme.colors.surfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Page ${index + 1}",
                        style = AppTheme.typography.bodyLarge,
                        color = AppTheme.colors.ink
                    )
                }
            }
        }

        ViewerTopBar(
            title = "Quarterly Financial Analysis 2026.pdf",
            currentPage = 4,
            pageCount = 40,
            visible = true,
            isBookmarked = false,
            isCondensed = isCondensed,
            onBackClick = {},
            onSearchClick = {},
            onToggleBookmark = {},
            onShareClick = {}
        )

        ViewerBottomBar(
            currentPage = 4,
            pageCount = 40,
            visible = true,
            hasOutline = true,
            isCondensed = isCondensed,
            onOutlineClick = {},
            onPagesClick = {},
            onAppearanceClick = {},
            onPageScrubbed = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
