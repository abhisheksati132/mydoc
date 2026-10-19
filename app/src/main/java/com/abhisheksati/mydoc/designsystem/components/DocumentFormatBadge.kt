package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.InterFontFamily
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors

/**
 * Clean, professional Google Drive / Files by Google style document format badge.
 * Provides instant recognition through soft-colored squircle containers and crisp typography.
 */
@Composable
fun DocumentFormatBadge(
    extension: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val colors = LocalAppColors.current
    val isDark = colors.background == DarkAppColors.background
    val tint = DocumentFormatColors.getTint(extension)
    val containerColor = DocumentFormatColors.getContainer(extension, isDark)
    val badgeShape = RoundedCornerShape(12.dp)

    val label = when (extension.lowercase()) {
        "pdf" -> "PDF"
        "docx", "doc" -> "DOC"
        "xlsx", "xls" -> "XLS"
        "csv" -> "CSV"
        "pptx", "ppt", "ppsx", "pps", "potx", "pot" -> "PPT"
        "txt", "log" -> "TXT"
        "md" -> "MD"
        "json" -> "JSON"
        "xml" -> "XML"
        "jpg", "jpeg", "png", "webp", "gif" -> "IMG"
        "mp4", "mkv", "mov" -> "VID"
        "mp3", "wav", "m4a", "flac" -> "AUD"
        else -> extension.take(3).uppercase().ifEmpty { "DOC" }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(badgeShape)
            .background(containerColor)
            .border(
                BorderStroke(
                    0.75.dp,
                    tint.copy(alpha = if (isDark) 0.35f else 0.25f)
                ),
                badgeShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = if (label.length > 3) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}
