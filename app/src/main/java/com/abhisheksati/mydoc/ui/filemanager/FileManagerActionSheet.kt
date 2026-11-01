package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleItemActionBottomSheet(
    item: StorageItem,
    isPinned: Boolean,
    onDismissRequest: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onExtractZip: () -> Unit,
    onCompressToZip: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint) = getFileTypeIcon(item.fileType)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.15f))
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.name,
                        style = typography.bodyStrong,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.formattedSize,
                        style = typography.micro,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            ItemActionOption(
                title = "Open",
                icon = Icons.Outlined.FolderOpen,
                onClick = onOpen
            )

            ItemActionOption(
                title = "Copy",
                icon = Icons.Outlined.ContentCopy,
                onClick = onCopy
            )

            ItemActionOption(
                title = "Move (Cut)",
                icon = Icons.Outlined.ContentCut,
                onClick = onMove
            )

            ItemActionOption(
                title = "Rename",
                icon = Icons.Outlined.DriveFileRenameOutline,
                onClick = onRename
            )

            ItemActionOption(
                title = "Move to Recycle Bin",
                icon = Icons.Outlined.Delete,
                tint = colors.error,
                onClick = onDelete
            )

            if (item.isDirectory) {
                ItemActionOption(
                    title = if (isPinned) "Unpin from Quick Access" else "Pin to Quick Access",
                    icon = if (isPinned) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                    tint = colors.accent,
                    onClick = onTogglePin
                )
            }

            val isZip = item.name.endsWith(".zip", ignoreCase = true) || item.name.endsWith(".jar", ignoreCase = true)
            if (isZip) {
                ItemActionOption(
                    title = "Extract ZIP Archive",
                    icon = Icons.Outlined.FolderZip,
                    tint = colors.accent,
                    onClick = onExtractZip
                )
            }

            ItemActionOption(
                title = "Compress to ZIP",
                icon = Icons.Outlined.Archive,
                onClick = onCompressToZip
            )

            ItemActionOption(
                title = "Details & Checksum",
                icon = Icons.Outlined.Info,
                onClick = onDetails
            )

            if (!item.isDirectory) {
                ItemActionOption(
                    title = "Share",
                    icon = Icons.Outlined.Share,
                    onClick = onShare
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ItemActionOption(
    title: String,
    icon: ImageVector,
    tint: Color = LocalAppColors.current.textPrimary,
    onClick: () -> Unit
) {
    val typography = LocalAppTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = typography.body,
            color = tint
        )
    }
}
