package com.abhisheksati.mydoc.ui.files

import com.abhisheksati.mydoc.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Build
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.StorageItem

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
    modifier: Modifier = Modifier,
    onOpenInTools: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = colors.scrim.copy(alpha = 0.45f),
        dragHandle = null,
        modifier = modifier
    ) {
        OverlaySystemBars()
        GlassSurface(
            shape = AppRadius.sheetShape,
            elevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Tactile Glass Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.inkMuted.copy(alpha = 0.35f))
                )

                // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Icon(
                    imageVector = getFileTypeIcon(item.fileType),
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = typography.documentName,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.formattedSize,
                        style = typography.caption,
                        color = colors.inkMuted
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

            if (!item.isDirectory && onOpenInTools != null) {
                ItemActionOption(
                    title = stringResource(R.string.action_open_pdf_tools),
                    icon = Icons.Outlined.Build,
                    tint = colors.accent,
                    onClick = onOpenInTools
                )
            }

            ItemActionOption(
                title = "Copy",
                icon = Icons.Outlined.ContentCopy,
                onClick = onCopy
            )

            ItemActionOption(
                title = stringResource(R.string.action_move_cut),
                icon = Icons.Outlined.ContentCut,
                onClick = onMove
            )

            ItemActionOption(
                title = stringResource(R.string.action_rename),
                icon = Icons.Outlined.DriveFileRenameOutline,
                onClick = onRename
            )

            ItemActionOption(
                title = stringResource(R.string.photo_move_to_bin_title),
                icon = Icons.Outlined.Delete,
                tint = colors.error,
                onClick = onDelete
            )

            if (item.isDirectory) {
                ItemActionOption(
                    title = stringResource(if (isPinned) R.string.action_unpin_quick_access else R.string.action_pin_quick_access),
                    icon = if (isPinned) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                    tint = colors.accent,
                    onClick = onTogglePin
                )
            }

            val isZip = item.name.endsWith(".zip", ignoreCase = true) || item.name.endsWith(".jar", ignoreCase = true)
            if (isZip) {
                ItemActionOption(
                    title = stringResource(R.string.action_extract_zip),
                    icon = Icons.Outlined.FolderZip,
                    tint = colors.accent,
                    onClick = onExtractZip
                )
            }

            ItemActionOption(
                title = stringResource(R.string.dialog_compress_zip),
                icon = Icons.Outlined.Archive,
                onClick = onCompressToZip
            )

            ItemActionOption(
                title = stringResource(R.string.action_details_checksum),
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
            .clip(AppRadius.pillShape)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
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
