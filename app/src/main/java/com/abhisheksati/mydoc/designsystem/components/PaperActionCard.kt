package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
internal fun PaperActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: Color = AppTheme.colors.accent
) {
    val colors = AppTheme.colors
    val shape = AppRadius.cardShape
    Column(
        modifier = modifier
            .heightIn(min = if (description == null) 82.dp else 148.dp)
            .clip(shape)
            .background(tone.copy(alpha = 0.10f), shape)
            .border(1.dp, tone.copy(alpha = 0.20f), shape)
            .pressScaleEffect(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp))
        Text(title, style = if (description == null) AppTheme.typography.bodyStrong else AppTheme.typography.titleSmall,
            color = colors.ink)
        if (description != null) Text(description, style = AppTheme.typography.caption, color = colors.inkMuted)
    }
}
