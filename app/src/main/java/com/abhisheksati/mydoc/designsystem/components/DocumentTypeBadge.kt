package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DocumentTypeGradients
import com.abhisheksati.mydoc.designsystem.tokens.InterFontFamily

/**
 * Apple-grade vibrant squircle document type badge with glass bevel and soft drop glow.
 */
@Composable
fun DocumentTypeBadge(
    extension: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    showIcon: Boolean = false
) {
    val cleanExt = extension.lowercase().removePrefix(".").trim()
    val gradient = DocumentTypeGradients.getGradientForExtension(cleanExt)
    val accentColor = DocumentTypeGradients.getAccentColorForExtension(cleanExt)
    val shape = RoundedCornerShape(12.dp)

    val label = when (cleanExt) {
        "pdf" -> "PDF"
        "doc", "docx" -> "DOC"
        "xls", "xlsx", "csv" -> "XLS"
        "ppt", "pptx" -> "PPT"
        "txt", "md" -> "TXT"
        "jpg", "jpeg", "png", "webp" -> "IMG"
        "mp4", "mkv" -> "VID"
        "mp3", "m4a", "wav" -> "AUD"
        else -> cleanExt.take(3).uppercase().ifEmpty { "DOC" }
    }

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // Soft colored ambient glow
                drawCircle(
                    color = accentColor.copy(alpha = 0.32f),
                    radius = this.size.maxDimension * 0.58f,
                    center = center
                )
            }
            .clip(shape)
            .background(gradient)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glossy bevel overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )

        if (showIcon) {
            val iconVector = when (cleanExt) {
                "pdf" -> Icons.Outlined.PictureAsPdf
                "doc", "docx" -> Icons.Outlined.Description
                "xls", "xlsx", "csv" -> Icons.Outlined.TableChart
                "ppt", "pptx" -> Icons.Outlined.Slideshow
                "jpg", "jpeg", "png", "webp" -> Icons.Outlined.Image
                "mp4", "mkv" -> Icons.Outlined.Movie
                "mp3", "m4a", "wav" -> Icons.Outlined.MusicNote
                else -> Icons.Outlined.Description
            }
            Icon(
                imageVector = iconVector,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        } else {
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (size < 40.dp) 10.sp else 12.sp,
                letterSpacing = 0.5.sp,
                color = Color.White
            )
        }
    }
}
