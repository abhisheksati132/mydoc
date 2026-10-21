package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Calculates smart reading time estimate based on file size and document type.
 */
fun calculateSmartReadingTime(sizeBytes: Long, pageCount: Int? = null): String {
    if (pageCount != null && pageCount > 0) {
        val minutes = (pageCount * 1.5).toInt().coerceAtLeast(1)
        return "⏱ ~$minutes min read"
    }
    // Heuristic estimation: ~300 words / 2KB per minute
    val estimatedMinutes = (sizeBytes / (35 * 1024L)).toInt().coerceIn(1, 90)
    return "⏱ ~$estimatedMinutes min read"
}

/**
 * Formats relative timestamp (e.g. Just now, 15m ago, 2h ago, 3d ago).
 */
fun formatRelativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0L)
    val seconds = diff / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Next-Grade Peek & Pop (Quick Look) Preview Modal inspired by Apple Quick Look & Google Files.
 * Pops up when a user long-presses any document card or row.
 */
@Composable
fun QuickLookModal(
    document: DocumentFile?,
    onDismiss: () -> Unit,
    onOpen: (DocumentFile) -> Unit,
    onShare: (DocumentFile) -> Unit,
    onToggleFavorite: (DocumentFile) -> Unit,
    onDelete: ((DocumentFile) -> Unit)? = null
) {
    if (document == null) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptics = rememberAppHaptics()
    val isOled = colors.background == DarkAppColors.background

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isOled) Color(0xFF101217) else colors.surfaceContainer,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 16.dp,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume inner click
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Look",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
                            color = colors.textSecondary
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hero Document Badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(colors.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        DocumentFormatBadge(
                            extension = document.typeBadge,
                            size = 54.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    Text(
                        text = document.displayName,
                        style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Contextual Metadata Chips Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = document.formattedSize,
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                        Text("•", color = colors.textSecondary)
                        Text(
                            text = if (document.lastOpenedTimestamp > 0) formatRelativeDate(document.lastOpenedTimestamp) else "Recent",
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                        Text("•", color = colors.textSecondary)
                        Text(
                            text = calculateSmartReadingTime(document.sizeBytes),
                            style = typography.caption.copy(fontWeight = FontWeight.Medium),
                            color = colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Action: Open Document Button
                    Button(
                        onClick = {
                            haptics.confirm()
                            onDismiss()
                            onOpen(document)
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Document",
                            style = typography.label.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Action Pills: Favorite, Share, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Favorite toggle
                        FilledTonalButton(
                            onClick = {
                                haptics.confirm()
                                onToggleFavorite(document)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (document.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (document.isFavorite) colors.accent else colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (document.isFavorite) "Saved" else "Save",
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                        }

                        // Share button
                        FilledTonalButton(
                            onClick = {
                                haptics.select()
                                onDismiss()
                                onShare(document)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share",
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                        }

                        // Delete button (if available)
                        if (onDelete != null) {
                            FilledTonalButton(
                                onClick = {
                                    haptics.reject()
                                    onDismiss()
                                    onDelete(document)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colors.error.copy(alpha = 0.12f),
                                    contentColor = colors.error
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = colors.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Trash",
                                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
