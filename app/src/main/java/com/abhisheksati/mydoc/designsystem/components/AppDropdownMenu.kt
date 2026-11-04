package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.glassRimBrush

/**
 * Apple Liquid Glass styled DropdownMenu:
 * - 16dp rounded card shape
 * - Translucent surface with specular 1dp gradient rim
 * - Soft tactile elevation shadow
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    val isDark = AppTheme.isDark
    val rimBrush = remember(colors, isDark) { glassRimBrush(colors, isDark) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = AppRadius.cardShape,
        containerColor = colors.surface.copy(alpha = if (AppTheme.isDark) 0.94f else 0.96f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, rimBrush),
        content = content
    )
}
