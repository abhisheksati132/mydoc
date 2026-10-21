package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFile

/**
 * Truncates filename in the middle so the extension is always preserved and visible.
 */
fun formatMiddleTruncated(name: String, maxChars: Int = 36): String {
    if (name.length <= maxChars) return name
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex <= 0) {
        val half = (maxChars - 1) / 2
        return name.take(half) + "…" + name.takeLast(half)
    }
    val ext = name.substring(dotIndex)
    val base = name.substring(0, dotIndex)
    val availableForBase = maxChars - ext.length - 1
    if (availableForBase <= 4) return name.take(maxChars - ext.length - 1) + "…" + ext

    val frontChars = (availableForBase * 0.6).toInt()
    val backChars = availableForBase - frontChars
    return base.take(frontChars) + "…" + base.takeLast(backChars) + ext
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppFileRow(
    displayName: String,
    typeBadge: String,
    metadataText: String,
    isFavorite: Boolean,
    progressFraction: Float? = null,
    progressText: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onMoveToShelf: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.confirm()
                    onToggleFavorite()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (onRemove != null) {
                        haptics.reject()
                        onRemove()
                        true
                    } else false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppSpacing.minTouchTarget)
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = onRemove != null,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val bg = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> colors.accent.copy(alpha = 0.16f)
                    SwipeToDismissBoxValue.EndToStart -> colors.error.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> if (isFavorite) Icons.Outlined.StarBorder else Icons.Outlined.Star
                    SwipeToDismissBoxValue.EndToStart -> Icons.Outlined.Delete
                    else -> null
                }
                val iconTint = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> colors.accent
                    SwipeToDismissBoxValue.EndToStart -> colors.error
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptics.snap()
                            onLongClick()
                        }
                    )
                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Recognizable Google-style format-colored badge
            DocumentFormatBadge(
                extension = typeBadge,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // File Name & Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatMiddleTruncated(displayName),
                    style = typography.body,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subText = if (!progressText.isNullOrBlank()) {
                    "$metadataText  •  $progressText"
                } else {
                    metadataText
                }

                Text(
                    text = subText,
                    style = typography.caption.withTabularFigures(),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Optional continue reading progress indicator
                if (progressFraction != null && progressFraction > 0f) {
                    Spacer(modifier = Modifier.height(AppSpacing.xSmall))
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(3.dp),
                        color = DocumentFormatColors.getTint(typeBadge),
                        trackColor = colors.surfaceVariant
                    )
                }
            }

            // Favorite button (48dp touch target)
            IconButton(
                onClick = {
                    haptics.confirm()
                    onToggleFavorite()
                },
                modifier = Modifier.size(AppSpacing.minTouchTarget)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (isFavorite) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Options menu button (48dp touch target)
            Box {
                IconButton(
                    onClick = {
                        haptics.select()
                        menuExpanded = true
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = colors.surfaceContainer,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    DropdownMenuItem(
                        text = { Text("Details & Info", style = typography.body) },
                        onClick = {
                            haptics.select()
                            menuExpanded = false
                            onDetails()
                        }
                    )
                    if (onMoveToShelf != null) {
                        DropdownMenuItem(
                            text = { Text("Move to shelf...", style = typography.body) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FolderSpecial,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                haptics.select()
                                menuExpanded = false
                                onMoveToShelf()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Share", style = typography.body) },
                        onClick = {
                            haptics.select()
                            menuExpanded = false
                            onShare()
                        }
                    )
                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from recents", style = typography.body) },
                            onClick = {
                                haptics.reject()
                                menuExpanded = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }

        // Standard hairline divider
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.dividerThickness)
                    .padding(start = AppSpacing.medium)
                    .border(AppSpacing.dividerThickness, colors.divider)
            )
        }
    }
}

@Composable
fun AppFileRow(
    document: DocumentFile,
    showContinueReading: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onShowDetails: () -> Unit,
    onMoveToShelf: (() -> Unit)? = null,
    onRemoveFromRecents: (() -> Unit)? = null,
    onLongClick: () -> Unit = onShowDetails,
    modifier: Modifier = Modifier
) {
    AppFileRow(
        displayName = document.displayName,
        typeBadge = document.typeBadge,
        metadataText = "${document.formattedSize}  •  ${document.formattedLastOpened}",
        isFavorite = document.isFavorite,
        progressFraction = if (showContinueReading && document.pageCount > 0) document.lastPage.toFloat() / document.pageCount else null,
        progressText = if (showContinueReading && document.pageCount > 0) "Page ${document.lastPage + 1} of ${document.pageCount}" else null,
        showDivider = showDivider,
        onClick = onClick,
        onLongClick = onLongClick,
        onToggleFavorite = onToggleFavorite,
        onShare = onShare,
        onDetails = onShowDetails,
        onMoveToShelf = onMoveToShelf,
        onRemove = onRemoveFromRecents,
        modifier = modifier
    )
}

