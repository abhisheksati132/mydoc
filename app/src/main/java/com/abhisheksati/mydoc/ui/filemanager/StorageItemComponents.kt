package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem

fun getFileTypeIcon(type: StorageFileType): Pair<ImageVector, Color> {
    return when (type) {
        StorageFileType.DIRECTORY -> Icons.Outlined.Folder to Color(0xFFFFB300)
        StorageFileType.PDF -> Icons.Outlined.PictureAsPdf to DocumentFormatColors.pdfTint
        StorageFileType.WORD -> Icons.Outlined.Description to DocumentFormatColors.wordTint
        StorageFileType.EXCEL -> Icons.Outlined.TableChart to DocumentFormatColors.excelTint
        StorageFileType.PPT -> Icons.Outlined.Slideshow to DocumentFormatColors.pptTint
        StorageFileType.IMAGE -> Icons.Outlined.Image to Color(0xFF9C27B0)
        StorageFileType.VIDEO -> Icons.Outlined.Movie to Color(0xFF009688)
        StorageFileType.AUDIO -> Icons.Outlined.AudioFile to Color(0xFFE91E63)
        StorageFileType.ARCHIVE -> Icons.Outlined.FolderZip to Color(0xFFFF5722)
        StorageFileType.APK -> Icons.Outlined.Android to Color(0xFF4CAF50)
        StorageFileType.CODE_TEXT -> Icons.Outlined.Code to Color(0xFF00ACC1)
        StorageFileType.GENERIC -> Icons.AutoMirrored.Outlined.InsertDriveFile to Color(0xFF9E9E9E)
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
    val (icon, tint) = getFileTypeIcon(item.fileType)

    val rowBackground = if (isSelected) {
        colors.accent.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }

    Surface(
        color = rowBackground,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
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
                    .clip(RoundedCornerShape(10.dp))
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
                    style = typography.body.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 15.sp
                    ),
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
                        style = typography.caption.copy(fontSize = 12.sp),
                        color = colors.textSecondary
                    )
                    if (item.formattedLastModified.isNotEmpty()) {
                        Text(
                            text = "  •  ${item.formattedLastModified}",
                            style = typography.caption.copy(fontSize = 12.sp),
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
    val isOled = colors.background == com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors.background
    val (icon, tint) = getFileTypeIcon(item.fileType)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.accentVariant else if (isOled) colors.surface else colors.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) colors.accent else colors.cardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
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
                        .clip(RoundedCornerShape(12.dp))
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
                    style = typography.body.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.formattedSize,
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )
            }
        }
    }
}
