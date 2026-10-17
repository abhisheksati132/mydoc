package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentTypeGradients
import com.abhisheksati.mydoc.designsystem.tokens.InterFontFamily
import com.abhisheksati.mydoc.domain.model.DocumentFile

/**
 * Apple-style 2-column Grid Card for documents.
 */
@Composable
fun DocumentGridCard(
    document: DocumentFile,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val accentColor = DocumentTypeGradients.getAccentColorForExtension(document.extension)
    val cardShape = RoundedCornerShape(AppSpacing.cardCornerRadius)

    val progressFraction = if (document.pageCount > 0 && document.lastPage > 0) {
        (document.lastPage.toFloat() / document.pageCount.toFloat()).coerceIn(0f, 1f)
    } else null

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.paddingSmall),
        shape = cardShape,
        backgroundColor = colors.cardBackground,
        borderColor = colors.glassBorder,
        ambientGlow = accentColor.copy(alpha = 0.18f),
        elevation = 4.dp,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Upper visual preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
                    .clip(RoundedCornerShape(topStart = AppSpacing.cardCornerRadius, topEnd = AppSpacing.cardCornerRadius))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f),
                                colors.surfaceVariant.copy(alpha = 0.35f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Central rich badge
                DocumentTypeBadge(
                    extension = document.extension,
                    size = 48.dp,
                    showIcon = true
                )

                // Top-right favorite icon
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) colors.accent else colors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Lower info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.paddingSmall)
            ) {
                Text(
                    text = document.displayName,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val meta = formatFileSize(document.sizeBytes)
                Text(
                    text = meta,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    color = colors.textSecondary
                )

                if (progressFraction != null && progressFraction > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = accentColor,
                        trackColor = colors.surfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.1f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
