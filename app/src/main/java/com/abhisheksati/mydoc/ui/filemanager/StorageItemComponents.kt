package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem

fun getFileTypeIcon(type: StorageFileType): ImageVector = when (type) {
    StorageFileType.DIRECTORY -> Icons.Outlined.Folder
    StorageFileType.PDF -> Icons.Outlined.PictureAsPdf
    StorageFileType.WORD -> Icons.Outlined.Description
    StorageFileType.EXCEL -> Icons.Outlined.TableChart
    StorageFileType.PPT -> Icons.Outlined.Slideshow
    StorageFileType.IMAGE -> Icons.Outlined.Image
    StorageFileType.VIDEO -> Icons.Outlined.Movie
    StorageFileType.AUDIO -> Icons.Outlined.AudioFile
    StorageFileType.ARCHIVE -> Icons.Outlined.FolderZip
    StorageFileType.APK -> Icons.Outlined.Android
    StorageFileType.CODE_TEXT -> Icons.Outlined.Code
    StorageFileType.GENERIC -> Icons.AutoMirrored.Outlined.InsertDriveFile
}

/**
 * One line of a directory: monochrome glyph, name, size and date, then a hairline.
 * Selection is an ink stamp on the row, not a floating card.
 */
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) colors.accent.copy(alpha = 0.08f) else Color.Transparent
            )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppSpacing.objectRowHeight)
                .padding(start = AppSpacing.screenPadding, end = AppSpacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = if (isSelected) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = "Select",
                    tint = if (isSelected) colors.accent else colors.inkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Icon(
                imageVector = getFileTypeIcon(item.fileType),
                contentDescription = item.fileType.name,
                tint = if (isSelected) colors.accent else colors.inkMuted,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = typography.bodyLargeStrong,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.formattedLastModified.isNotEmpty()) {
                        "${item.formattedSize} · ${item.formattedLastModified}"
                    } else {
                        item.formattedSize
                    },
                    style = typography.caption.withTabularFigures(),
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = colors.inkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding)
                .height(AppSpacing.hairline)
                .background(colors.line)
        )
    }
}

/**
 * Grid cell on the same terms as the row: a glyph, the name, the size, and an
 * ink stamp when selected. The grid keeps the user's choice of density, not a
 * second visual language.
 */
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
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .background(
                if (isSelected) colors.accent.copy(alpha = 0.08f) else Color.Transparent
            )
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
                .padding(AppSpacing.small)
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = "Selected",
                    tint = if (isSelected) colors.accent else colors.inkMuted,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                )
            } else {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = colors.inkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = getFileTypeIcon(item.fileType),
                    contentDescription = item.fileType.name,
                    tint = if (isSelected) colors.accent else colors.ink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(
                    text = item.name,
                    style = typography.bodyStrong,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppSpacing.xSmall))
                Text(
                    text = item.formattedSize,
                    style = typography.caption.withTabularFigures(),
                    color = colors.inkMuted
                )
            }
        }
    }
}
