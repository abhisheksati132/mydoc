package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Minimal, typographic section header adhering to Apple / Linear design standards.
 * Pure typography and generous whitespace with zero pill boxes, borders, or gimmicky dots.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.medium,
                end = AppSpacing.medium,
                top = 20.dp,
                bottom = 6.dp
            )
    ) {
        Text(
            text = title,
            style = typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = colors.textSecondary
        )
    }
}
