package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
fun AppCircularProgress(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    val colors = AppTheme.colors
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = colors.accent,
        strokeWidth = strokeWidth,
        trackColor = colors.surfaceVariant
    )
}

@Composable
fun AppProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp
) {
    AppCircularProgress(modifier = modifier, size = size, strokeWidth = strokeWidth)
}

@Composable
fun AppLinearProgress(
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
                .fillMaxWidth()
                .height(2.dp),
            color = colors.accent,
            trackColor = colors.surfaceVariant
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(2.dp),
            color = colors.accent,
            trackColor = colors.surfaceVariant
        )
    }
}
