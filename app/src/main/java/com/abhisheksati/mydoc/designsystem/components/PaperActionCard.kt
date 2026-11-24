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
    Box(
        modifier = modifier
            .heightIn(min = if (description == null) 104.dp else 184.dp)
            .pressScaleEffect(targetScaleOnPress = 0.965f, onClick = onClick)
            .clip(shape)
            .background(colors.surface, shape)
            .background(tone.copy(alpha = if (AppTheme.isDark) 0.18f else 0.12f), shape)
            .border(1.dp, tone.copy(alpha = 0.14f), shape)
    ) {
        Icon(icon, contentDescription = null, tint = tone.copy(alpha = 0.10f),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                .size(if (description == null) 64.dp else 104.dp))
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).background(colors.surface.copy(alpha = 0.7f), AppRadius.insetShape),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(21.dp))
            }
            if (description != null) Spacer(Modifier.height(12.dp))
            Text(title, style = if (description == null) AppTheme.typography.bodyStrong else AppTheme.typography.heading,
                color = colors.ink)
            if (description != null) Text(description, style = AppTheme.typography.caption, color = colors.inkSecondary)
        }
    }
}
