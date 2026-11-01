package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.DocumentFilter

/**
 * The document format filter row. Home and Files render this same control:
 * the selected chip is a solid ink block, the rest sit on the container surface.
 */
@Composable
fun DocumentFilterChips(
    selected: DocumentFilter,
    onSelected: (DocumentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DocumentFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            val chipBackground by animateColorAsState(
                targetValue = if (isSelected) colors.ink else colors.surfaceContainer,
                animationSpec = tween(durationMillis = AppMotion.FAST),
                label = "documentFilterChipBackground"
            )
            val chipText by animateColorAsState(
                targetValue = if (isSelected) colors.background else colors.inkMuted,
                animationSpec = tween(durationMillis = AppMotion.FAST),
                label = "documentFilterChipText"
            )

            Surface(
                shape = AppRadius.pillShape,
                color = chipBackground,
                modifier = Modifier
                    .clip(AppRadius.pillShape)
                    .pressScaleEffect { onSelected(filter) }
            ) {
                Text(
                    text = filter.label,
                    style = if (isSelected) typography.captionStrong else typography.caption,
                    color = chipText,
                    modifier = Modifier.padding(
                        horizontal = AppSpacing.medium,
                        vertical = AppSpacing.small
                    )
                )
            }
        }
    }
}
