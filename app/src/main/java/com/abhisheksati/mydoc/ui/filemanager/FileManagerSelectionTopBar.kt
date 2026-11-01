package com.abhisheksati.mydoc.ui.filemanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

@Composable
fun FileManagerSelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onShare: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCompress: () -> Unit,
    onRename: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    var isSelectionMenuOpen by remember { mutableStateOf(false) }

    Surface(
        color = colors.background,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textPrimary)
                }
                Text(
                    text = "$selectedCount selected",
                    style = typography.bodyStrong,
                    color = colors.textPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Share
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", tint = colors.textPrimary)
                }

                // 2. Move (Cut)
                IconButton(onClick = onCut) {
                    Icon(Icons.Outlined.ContentCut, contentDescription = "Move", tint = colors.textPrimary)
                }

                // 3. Delete
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = colors.error)
                }

                // 4. Overflow Menu (Safe 48dp touch targets)
                Box {
                    IconButton(onClick = { isSelectionMenuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More actions", tint = colors.textPrimary)
                    }

                    DropdownMenu(
                        expanded = isSelectionMenuOpen,
                        onDismissRequest = { isSelectionMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select All", style = typography.body) },
                            leadingIcon = { Icon(Icons.Outlined.SelectAll, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                isSelectionMenuOpen = false
                                onSelectAll()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy", style = typography.body) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                isSelectionMenuOpen = false
                                onCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compress to ZIP", style = typography.body) },
                            leadingIcon = { Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                isSelectionMenuOpen = false
                                onCompress()
                            }
                        )
                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text("Rename", style = typography.body) },
                                leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, tint = colors.accent) },
                                onClick = {
                                    isSelectionMenuOpen = false
                                    onRename()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
