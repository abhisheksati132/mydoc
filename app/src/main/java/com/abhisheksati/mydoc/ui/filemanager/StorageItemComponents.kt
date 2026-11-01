package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem

@Composable
fun getFileTypeIcon(type: StorageFileType): Pair<ImageVector, Color> {
    val colors = LocalAppColors.current
    return when (type) {
        StorageFileType.DIRECTORY -> Icons.Outlined.Folder to colors.accent
        StorageFileType.PDF -> Icons.Outlined.PictureAsPdf to DocumentFormatColors.pdfTint
        StorageFileType.WORD -> Icons.Outlined.Description to DocumentFormatColors.wordTint
        StorageFileType.EXCEL -> Icons.Outlined.TableChart to DocumentFormatColors.excelTint
        StorageFileType.PPT -> Icons.Outlined.Slideshow to DocumentFormatColors.pptTint
        StorageFileType.IMAGE -> Icons.Outlined.Image to DocumentFormatColors.mediaTint
        StorageFileType.VIDEO -> Icons.Outlined.Movie to DocumentFormatColors.mediaTint
        StorageFileType.AUDIO -> Icons.Outlined.AudioFile to DocumentFormatColors.mediaTint
        StorageFileType.ARCHIVE -> Icons.Outlined.FolderZip to DocumentFormatColors.zipTint
        StorageFileType.APK -> Icons.Outlined.Android to colors.accent
        StorageFileType.CODE_TEXT -> Icons.Outlined.Code to DocumentFormatColors.textTint
        StorageFileType.GENERIC -> Icons.AutoMirrored.Outlined.InsertDriveFile to DocumentFormatColors.textTint
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StorageItemRow(
    item: StorageItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val (icon, tint) = getFileTypeIcon(item.fileType)

    val rowBackground = if (isSelected) {
        colors.accent.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }

    Surface(
        color = rowBackground,
        shape = AppRadius.cardShape,
        border = if (isSelected) BorderStroke(AppSpacing.hairline, colors.accent.copy(alpha = 0.35f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .pressScale(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = {
                    haptics.snap()
                    onLongClick()
                }
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Selection Checkbox
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Select",
                    tint = if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(22.dp)
                )
            }

            // Always Visible Folder / File Icon Box
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(AppRadius.cardShape)
                    .background(tint.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.fileType.name,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))

            // Name & Subtext
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = if (isSelected) typography.bodyStrong else typography.body,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.formattedSize,
                        style = typography.caption.withTabularFigures(),
                        color = colors.textSecondary
                    )
                    if (item.formattedLastModified.isNotEmpty()) {
                        Text(
                            text = "  •  ${item.formattedLastModified}",
                            style = typography.caption.withTabularFigures(),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Trailing Kebab (hidden in selection mode)
            if (!isSelectionMode) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StorageItemGrid(
    item: StorageItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = LocalIsOled.current
    val haptics = rememberAppHaptics()
    val (icon, tint) = getFileTypeIcon(item.fileType)

    val interactionSource = remember { MutableInteractionSource() }

    Card(
        shape = AppRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.primaryContainer else if (isOled) colors.surface else colors.surfaceContainer
        ),
        border = BorderStroke(
            AppSpacing.hairline,
            if (isSelected) colors.accent else colors.cardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clip(AppRadius.cardShape)
            .pressScale(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = {
                    haptics.snap()
                    onLongClick()
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Trailing Selection Badge or More Kebab
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelectionMode,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Selected",
                    tint = if (isSelected) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!isSelectionMode) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(AppRadius.cardShape)
                        .background(tint.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.fileType.name,
                        tint = tint,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.name,
                    style = if (isSelected) typography.bodyStrong else typography.label,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.formattedSize,
                    style = typography.micro.withTabularFigures(),
                    color = colors.textSecondary
                )
            }
        }
    }
}
