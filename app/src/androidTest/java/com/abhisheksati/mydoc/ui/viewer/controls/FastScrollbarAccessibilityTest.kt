package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FastScrollbarAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun scrollbar_announcesPositionAndExposesPageActions() {
        var selected = -1
        compose.setContent {
            AppDesignSystemTheme {
                FastScrollbar(
                    currentPage = 2,
                    pageCount = 10,
                    onPageSelected = { selected = it },
                    modifier = Modifier.height(400.dp)
                )
            }
        }
        val node = compose.onNodeWithContentDescription("Page 3 of 10").assertExists()
        val actions = node.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(actions.first { it.label == "Next page" }.action.invoke())
        assertEquals(3, selected)
        assertTrue(actions.first { it.label == "Previous page" }.action.invoke())
        assertEquals(1, selected)
    }

    @Test
    fun firstPage_previousActionIsUnavailable() {
        compose.setContent {
            AppDesignSystemTheme {
                FastScrollbar(
                    currentPage = 0,
                    pageCount = 10,
                    onPageSelected = {},
                    modifier = Modifier.height(400.dp)
                )
            }
        }
        val actions = compose.onNodeWithContentDescription("Page 1 of 10")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertFalse(actions.first { it.label == "Previous page" }.action.invoke())
    }
}