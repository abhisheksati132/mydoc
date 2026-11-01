package com.abhisheksati.mydoc.ui.viewer.pdf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.ReadingMode

/**
 * Floating contextual action pill shown at the top of the PDF viewer
 * when text is highlighted or a page is selected.
 */
@Composable
fun PdfFloatingContextualBar(
    pageNum: Int,
    activeSelectionText: String?,
    readingMode: ReadingMode,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onSpeak: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 64.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = AppRadius.roundShape,
            color = colors.surface.copy(alpha = 0.95f),
            border = BorderStroke(0.5.dp, colors.line),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Copy
                FloatingPillItem(
                    icon = Icons.Outlined.ContentCopy,
                    label = if (activeSelectionText != null) "Copy" else "Copy Page",
                    tint = colors.accent,
                    onClick = onCopy
                )

                // 2. Share
                FloatingPillItem(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    tint = DocumentFormatColors.excelTint,
                    onClick = onShare
                )

                // 3. Web Search
                FloatingPillItem(
                    icon = Icons.Outlined.TextFields,
                    label = "Search",
                    tint = colors.accent,
                    onClick = onSearch
                )

                // 4. Read Aloud (TTS)
                FloatingPillItem(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    label = "Speak",
                    tint = DocumentFormatColors.zipTint,
                    onClick = onSpeak
                )

                // Close button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", style = typography.captionStrong, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun FloatingPillItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val typography = LocalAppTypography.current
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .clip(AppRadius.sheetShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = typography.captionStrong, color = colors.textPrimary)
    }
}
