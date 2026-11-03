package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The signature gradient rule: a 3dp spectrum line that closes headers and
 * replaces the old ink rule. One consistent stroke of brand color everywhere.
 */
@Composable
fun VividRule(modifier: Modifier = Modifier, thickness: Dp = 1.dp) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
