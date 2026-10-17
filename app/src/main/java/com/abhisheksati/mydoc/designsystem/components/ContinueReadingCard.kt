package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * Apple Books style "Continue Reading" Hero Glass Card.
 */
@Composable
fun ContinueReadingCard(
    document: DocumentFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val haptic = LocalHapticFeedback.current
    val accentColor = DocumentTypeGradients.getAccentColorForExtension(document.extension)
    val cardShape = RoundedCornerShape(AppSpacing.cardCornerRadius)

    val progressFraction = if (document.pageCount > 0 && document.lastPage > 0) {
        (document.lastPage.toFloat() / document.pageCount.toFloat()).coerceIn(0f, 1f)
    } else null

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingSmall),
        shape = cardShape,
        backgroundColor = colors.cardBackground,
        borderColor = colors.glassBorder,
        ambientGlow = accentColor,
        elevation = 8.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.paddingMedium)
        ) {
            // Header Row: Badge, Category Label, and Resume Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    DocumentTypeBadge(
                        extension = document.extension,
                        size = 38.dp
                    )

                    Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CONTINUE READING",
                                fontSize = 10.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = accentColor
                            )
                        }

                        Text(
                            text = formatMiddleTruncated(document.displayName, 28),
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Apple-style Frosted "Resume" pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppSpacing.pillCornerRadius))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.22f),
                                    accentColor.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.40f),
                            shape = RoundedCornerShape(AppSpacing.pillCornerRadius)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "Resume",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resume",
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
            }

            // Progress Bar & Reading Stat
            Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

            val progressPercent = if (progressFraction != null) {
                (progressFraction * 100).toInt()
            } else 0

            val readDetailText = if (document.pageCount > 0 && document.lastPage > 0) {
                "Page ${document.lastPage + 1} of ${document.pageCount}  •  $progressPercent% completed"
            } else {
                "Tap to continue reading from where you left off"
            }

            if (progressFraction != null && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = colors.surfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = readDetailText,
                fontSize = 11.sp,
                fontFamily = InterFontFamily,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
