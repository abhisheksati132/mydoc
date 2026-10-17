package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors

/**
 * Standard hairline divider component using design system tokens.
 * Elevation = 0; dividers separate surfaces cleanly.
 */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = AppSpacing.dividerThickness
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(colors.divider)
    )
}
