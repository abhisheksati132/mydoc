package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.medium,
                end = AppSpacing.medium,
                top = AppSpacing.medium,
                bottom = AppSpacing.small
            )
    ) {
        Text(
            text = title.uppercase(),
            style = typography.label.copy(letterSpacing = 1.sp),
            color = colors.textSecondary
        )
    }
}
