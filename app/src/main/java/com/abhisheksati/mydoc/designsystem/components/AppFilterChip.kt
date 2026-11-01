package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tool-options chip. Kept as a named alias because tool sheets read worse with a
 * generic name; it renders [AppChip] like every other chip in the app.
 */
@Composable
fun AppFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    AppChip(
        label = label,
        selected = isSelected,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon
    )
}
