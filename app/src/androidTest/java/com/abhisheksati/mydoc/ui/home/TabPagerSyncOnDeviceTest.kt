package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.abhisheksati.mydoc.domain.model.HomeTab
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TabPagerSyncOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    private val tabs = listOf(HomeTab.HOME, HomeTab.LIBRARY, HomeTab.TOOLS)
    private lateinit var pagerState: PagerState
    private lateinit var scope: kotlinx.coroutines.CoroutineScope
    private var currentTab by mutableStateOf(HomeTab.HOME)
    private var mountSync by mutableStateOf(true)

    private fun setPagerContent() {
        compose.setContent {
            pagerState = rememberPagerState(initialPage = 0) { tabs.size }
            scope = androidx.compose.runtime.rememberCoroutineScope()
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Box(Modifier.fillMaxSize()) { androidx.compose.material3.Text("Page $page") }
            }
            if (mountSync) {
                SyncTabPager(
                    pagerState = pagerState,
                    currentTab = currentTab,
                    tabs = tabs,
                    reducedMotion = false,
                    onTabSettled = { currentTab = it }
                )
            }
        }
    }

    private fun awaitPage(page: Int) {
        compose.waitUntil(5_000) {
            pagerState.currentPage == page && !pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction == 0f
        }
        compose.waitForIdle()
    }

    @Test fun multiPageTabJumpSettlesOnTargetWithoutStranding() {
        setPagerContent()
        compose.runOnIdle { currentTab = HomeTab.TOOLS }
        awaitPage(2)
        assertEquals(HomeTab.TOOLS, currentTab)
    }

    @Test fun restoredFractionalOffsetSnapsBackToTheSelectedTab() {
        mountSync = false
        setPagerContent()
        compose.runOnIdle { scope.launch { pagerState.scrollToPage(0, 0.4f) } }
        compose.waitUntil(5_000) { pagerState.currentPageOffsetFraction == 0.4f }
        compose.runOnIdle { mountSync = true }
        awaitPage(0)
        assertEquals(HomeTab.HOME, currentTab)
    }

    @Test fun userSwipeUpdatesTheSelectedTab() {
        setPagerContent()
        compose.onNode(hasScrollAction())
            .performTouchInput { swipeLeft() }
        awaitPage(1)
        compose.waitUntil(5_000) { currentTab == HomeTab.LIBRARY }
    }
    @Test fun rapidTabChangesFinishOnTheLastRequestedTab() {
        setPagerContent()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { currentTab = HomeTab.TOOLS }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { currentTab = HomeTab.HOME }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { currentTab = HomeTab.LIBRARY }
        compose.mainClock.autoAdvance = true
        awaitPage(1)
        assertEquals(HomeTab.LIBRARY, currentTab)
    }

}