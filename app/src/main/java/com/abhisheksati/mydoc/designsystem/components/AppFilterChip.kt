package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    // Ensure 48dp minimum touch target
    Box(
        modifier = modifier.defaultMinSize(minHeight = AppSpacing.minTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (selected) colors.surfaceVariant else Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selected) colors.accent.copy(alpha = 0.4f) else colors.divider.copy(alpha = 0.6f)
            ),
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    androidx.compose.material3.Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (selected) colors.accent else colors.textSecondary,
                        modifier = Modifier.defaultMinSize(16.dp, 16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = typography.label,
                    color = if (selected) colors.textPrimary else colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun AppFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    AppFilterChip(
        selected = isSelected,
        onClick = onClick,
        label = label,
        leadingIcon = leadingIcon,
        modifier = modifier
    )
}

