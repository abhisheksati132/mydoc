package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
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
    val shape = AppRadius.xLargeShape
    Column(
        modifier = modifier
            .heightIn(min = if (description == null) 82.dp else 148.dp)
            .clip(shape)
            .background(colors.surface, shape)
            .background(Brush.verticalGradient(listOf(tone.copy(alpha = 0.13f), tone.copy(alpha = 0.04f))), shape)
            .border(1.dp, tone.copy(alpha = 0.20f), shape)
            .pressScaleEffect(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(34.dp).background(tone.copy(alpha = 0.16f), AppRadius.insetShape),
            contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
        Text(title, style = if (description == null) AppTheme.typography.bodyStrong else AppTheme.typography.titleSmall,
            color = colors.ink)
        if (description != null) Text(description, style = AppTheme.typography.caption, color = colors.inkMuted)
    }
}
