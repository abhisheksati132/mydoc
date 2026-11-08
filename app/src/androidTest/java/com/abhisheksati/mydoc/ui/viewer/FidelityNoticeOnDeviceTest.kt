package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Rule
import org.junit.Test

class FidelityNoticeOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    private fun checkDismiss(direction: LayoutDirection) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides direction) {
                AppDesignSystemTheme {
                    FidelityNotice(FidelityDisclosure("Re-laid out for reading", listOf("Formatting may differ from the original.")))
                }
            }
        }
        compose.onNodeWithText("Re-laid out for reading").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dismiss rendering notice").assertIsDisplayed().performClick()
        compose.onNodeWithText("Re-laid out for reading").assertDoesNotExist()
    }

    @Test fun noticeCanBeDismissedWithLargeText() = checkDismiss(LayoutDirection.Ltr)
    @Test fun noticeCanBeDismissedWithLargeRtlText() = checkDismiss(LayoutDirection.Rtl)

    @Test fun newlyReportedLimitationsAreShownAfterDismissal() {
        val disclosure = mutableStateOf(FidelityDisclosure("Re-laid out for reading", emptyList()))
        compose.setContent { AppDesignSystemTheme { FidelityNotice(disclosure.value) } }
        compose.onNodeWithContentDescription("Dismiss rendering notice").performClick()
        compose.runOnIdle { disclosure.value = FidelityDisclosure("Only part of this file was read", listOf("Some pages are missing.")) }
        compose.onNodeWithText("Only part of this file was read").assertIsDisplayed()
    }

    @Test fun openingAnotherDocumentShowsItsNoticeEvenWithSameLimitations() {
        val document = mutableStateOf("first-document")
        compose.setContent { AppDesignSystemTheme {
            FidelityNotice(FidelityDisclosure("Re-laid out for reading", emptyList()), dismissalKey = document.value)
        } }
        compose.onNodeWithContentDescription("Dismiss rendering notice").performClick()
        compose.runOnIdle { document.value = "second-document" }
        compose.onNodeWithText("Re-laid out for reading").assertIsDisplayed()
    }
}
