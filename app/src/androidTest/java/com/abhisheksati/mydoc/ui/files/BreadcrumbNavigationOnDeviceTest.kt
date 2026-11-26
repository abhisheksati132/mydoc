package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.DeviceCaptures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BreadcrumbNavigationOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    private val parents = listOf("Internal storage", "Documents and references", "Project archives")

    @Test fun deepPathRevealsAndMarksTheCurrentFolder() {
        compose.setContent { AppDesignSystemTheme {
            Box(Modifier.width(288.dp)) { BreadcrumbRow((parents + "Current folder").map { it to {} }) }
        } }
        compose.onNodeWithText("Current folder").assertIsDisplayed().assertIsSelected()
        DeviceCaptures.save("breadcrumb-current", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test fun ancestorRemainsReachableAndSameDepthNavigationRevealsTheNewFolder() {
        var current by mutableStateOf("First folder")
        var opened = ""
        compose.setContent { AppDesignSystemTheme {
            Box(Modifier.width(288.dp)) {
                BreadcrumbRow((parents + current).map { label -> label to { opened = label } })
            }
        } }
        compose.onNodeWithText("Internal storage").performScrollTo().performClick()
        assertEquals("Internal storage", opened)
        compose.runOnIdle { current = "Next folder" }
        compose.onNodeWithText("Next folder").assertIsDisplayed().assertIsSelected()
    }

    @Test fun currentFolderIsVisibleWithLargeTextAndMirroredLayout() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppDesignSystemTheme {
                    Box(Modifier.width(288.dp)) { BreadcrumbRow((parents + "Current").map { it to {} }) }
                }
            }
        }
        compose.onNodeWithText("Current").assertIsDisplayed().assertIsSelected()
        DeviceCaptures.save("breadcrumb-large-rtl", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test fun narrowingTheWindowRevealsTheCurrentFolderAgain() {
        var width by mutableStateOf(340.dp)
        compose.setContent { AppDesignSystemTheme {
            Box(Modifier.width(width)) { BreadcrumbRow((parents + "Current folder").map { it to {} }) }
        } }
        compose.onNodeWithText("Internal storage").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { width = 220.dp }
        compose.onNodeWithText("Current folder").assertIsDisplayed().assertIsSelected()
    }
}
