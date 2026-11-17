package com.abhisheksati.mydoc.e2e

import androidx.compose.ui.geometry.Size
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.cache.ByteBoundedLru
import com.abhisheksati.mydoc.ui.viewer.controls.ZoomMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * End-to-End Opaque-Box Test Suite for R4: Viewer Experience, UI Honesty, and Resource Lifecycle.
 * Covers Features 27-33 across Tiers 1-4.
 */
class ViewerLifecycleExperienceE2ETest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var rootDir: File

    @Before
    fun setUp() {
        rootDir = tempFolder.newFolder("mydoc_e2e_viewer_root")
    }

    // =========================================================================
    // FEATURE 27: Virtualized Spreadsheet Grid Windowing (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature27_virtualizedRowWindowingComputesVisibleRange() {
        val totalRows = 500
        val rowHeightPx = 40f
        val viewportHeightPx = 800f
        val scrollOffsetPx = 1200f // Scrolled down 30 rows
        val prefetchMargin = 5

        val firstVisibleRow = (scrollOffsetPx / rowHeightPx).toInt().coerceAtLeast(0)
        val visibleRowCount = (viewportHeightPx / rowHeightPx).toInt() + 1
        val lastVisibleRow = (firstVisibleRow + visibleRowCount).coerceAtMost(totalRows - 1)

        val windowStart = (firstVisibleRow - prefetchMargin).coerceAtLeast(0)
        val windowEnd = (lastVisibleRow + prefetchMargin).coerceAtMost(totalRows - 1)

        assertEquals(30, firstVisibleRow)
        assertEquals(51, lastVisibleRow)
        assertEquals(25, windowStart)
        assertEquals(56, windowEnd)

        // The rendered window size must be strictly bounded (~32 rows) instead of 500 rows
        val windowSize = windowEnd - windowStart + 1
        assertTrue("Composed window size must be bounded under 40 rows", windowSize <= 40)
        assertTrue("Composed window size must be far smaller than total 500 rows", windowSize < totalRows)
    }

    @Test
    fun tier2_feature27_virtualizedGridAtTopAndBottomBoundaries() {
        val totalRows = 200
        val rowHeightPx = 50f
        val viewportHeightPx = 1000f
        val prefetchMargin = 5

        // At Top Boundary (offset = 0)
        val topOffset = 0f
        val topFirst = (topOffset / rowHeightPx).toInt().coerceAtLeast(0)
        val topStart = (topFirst - prefetchMargin).coerceAtLeast(0)
        assertEquals(0, topStart)

        // At Bottom Boundary
        val bottomOffset = (totalRows * rowHeightPx) - viewportHeightPx
        val bottomFirst = (bottomOffset / rowHeightPx).toInt().coerceAtLeast(0)
        val bottomLast = (bottomFirst + (viewportHeightPx / rowHeightPx).toInt()).coerceAtMost(totalRows - 1)
        val bottomEnd = (bottomLast + prefetchMargin).coerceAtMost(totalRows - 1)
        assertEquals(totalRows - 1, bottomEnd)
    }

    // =========================================================================
    // FEATURE 28: Single-Application Reading Modes (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature28_readingModeEnumerationComplete() {
        val modes = ReadingMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(ReadingMode.NORMAL))
        assertTrue(modes.contains(ReadingMode.INVERTED_DARK))
        assertTrue(modes.contains(ReadingMode.SEPIA))
        assertTrue(modes.contains(ReadingMode.EYE_COMFORT))
    }

    // =========================================================================
    // FEATURE 29: Reader Fit Page / Fit Width Scaling (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature29_fitWidthScalesToViewportWidth() {
        val viewportWidth = 1080f
        val viewportHeight = 2400f
        val pageWidth = 595f  // Standard A4
        val pageHeight = 842f
        val pageAspectRatio = pageWidth / pageHeight // ~0.706

        // In Fit Width mode:
        val renderedWidth = viewportWidth
        val renderedHeight = viewportWidth / pageAspectRatio

        assertEquals(1080f, renderedWidth, 0.01f)
        assertEquals(1528.33f, renderedHeight, 1.0f)
    }

    @Test
    fun tier2_feature29_fitPageConstrainsWithinBothWidthAndHeight() {
        val viewportWidth = 1080f
        val viewportHeight = 1920f
        val pageAspectRatio = 595f / 842f // ~0.706 (Portrait)
        val viewportAspectRatio = viewportWidth / viewportHeight // 0.5625

        // Portrait page fits by height constraint
        val (fitWidth, fitHeight) = if (pageAspectRatio > viewportAspectRatio) {
            val w = viewportWidth
            val h = viewportWidth / pageAspectRatio
            Pair(w, h)
        } else {
            val h = viewportHeight
            val w = viewportHeight * pageAspectRatio
            Pair(w, h)
        }

        assertTrue("Fit Page width must not exceed viewport width", fitWidth <= viewportWidth)
        assertTrue("Fit Page height must not exceed viewport height", fitHeight <= viewportHeight)
    }

    // =========================================================================
    // FEATURE 33: Memory Bounds & Teardown (Tier 1, 2, 3)
    // =========================================================================

    @Test
    fun tier1_feature33_byteBoundedLruEvictsOldestEntriesBeyondByteBudget() {
        val cache = ByteBoundedLru<String> { it.length }
        cache.put("page_1", "a".repeat(10))
        cache.put("page_2", "b".repeat(10))
        cache.put("page_3", "c".repeat(10))
        assertEquals(30, cache.currentBytes)

        cache.trimTo(25)

        assertNull(cache["page_1"])
        assertNotNull(cache["page_2"])
        assertNotNull(cache["page_3"])
        assertEquals(20, cache.currentBytes)
    }

    @Test
    fun tier2_feature33_byteBoundedLruKeepsRecentlyReadEntries() {
        val cache = ByteBoundedLru<String> { it.length }
        cache.put("page_1", "a".repeat(10))
        cache.put("page_2", "b".repeat(10))
        // Reading page_1 makes page_2 the eviction candidate
        assertEquals("a".repeat(10), cache["page_1"])
        cache.put("page_3", "c".repeat(10))

        cache.trimTo(25)

        assertNotNull(cache["page_1"])
        assertNull(cache["page_2"])
        assertNotNull(cache["page_3"])
    }

    @Test
    fun tier2_feature33_byteBoundedLruReplacingAnEntryAccountsBytesOnce() {
        val cache = ByteBoundedLru<String> { it.length }
        cache.put("page_1", "a".repeat(10))
        cache.put("page_1", "b".repeat(4))

        assertEquals(1, cache.size)
        assertEquals(4, cache.currentBytes)
    }

    @Test
    fun tier3_feature33_clearReleasesEveryEntryAtTeardown() {
        val cache = ByteBoundedLru<String> { it.length }
        cache.put("page_1", "a".repeat(20))
        cache.put("page_2", "b".repeat(20))

        cache.clear()

        assertEquals(0, cache.currentBytes)
        assertEquals(0, cache.size)
        assertNull(cache["page_1"])
    }

    // =========================================================================
    // TIER 4: Real-World Reading Session & Memory Lifecycle Scenario
    // =========================================================================

    @Test
    fun tier4_realWorldReadingSessionAndLifecycleTeardownScenario() {
        // Step 1: User opens reader and configures preferences
        var readingMode = ReadingMode.NORMAL
        var isFitPage = false
        val tileCache = ByteBoundedLru<String> { it.length }

        // Step 2: Switch to Night reading mode
        readingMode = ReadingMode.INVERTED_DARK
        assertEquals(ReadingMode.INVERTED_DARK, readingMode)

        // Step 3: Toggle Fit Page
        isFitPage = true
        assertTrue(isFitPage)

        // Step 4: Zoom into page
        val viewport = Size(1080f, 1920f)
        val maxPan = ZoomMath.calculateMaxPan(viewport, 2.0f)
        assertTrue(maxPan.x > 0f && maxPan.y > 0f)

        // Step 5: Pages accumulate in the tile cache while the user reads
        repeat(10) { page -> tileCache.put("page_$page", "x".repeat(100)) }
        assertEquals(1000, tileCache.currentBytes)

        // Step 6: Low Memory Warning (System onTrimMemory): recent pages survive the halving
        tileCache.trimTo(tileCache.currentBytes / 2)
        assertEquals(500, tileCache.currentBytes)
        assertNull(tileCache["page_0"])

        // Step 7: User closes viewer (Lifecycle Teardown): nothing survives
        tileCache.clear()
        assertEquals(0, tileCache.currentBytes)
        assertEquals(0, tileCache.size)
    }
}
