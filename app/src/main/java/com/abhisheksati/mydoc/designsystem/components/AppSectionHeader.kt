package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Architectural Eyebrow Badge Section Header (Stitch & High-End Design Taste).
 * Renders an ultra-refined microscopic pill badge with whisper border and status dot.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.medium,
                end = AppSpacing.medium,
                top = 16.dp,
                bottom = 8.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isOled) Color(0xFF141418) else colors.surfaceVariant.copy(alpha = 0.7f)
                )
                .border(
                    BorderStroke(
                        0.75.dp,
                        if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)
                    ),
                    RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
            Text(
                text = title.uppercase(),
                style = typography.caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp
                ),
                color = if (isOled) Color(0xFFE4E4E7) else colors.textPrimary.copy(alpha = 0.85f)
            )
        }
    }
}
