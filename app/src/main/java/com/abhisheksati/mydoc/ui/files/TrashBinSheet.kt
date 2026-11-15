package com.abhisheksati.mydoc.ui.files

import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.TrashItem
import com.abhisheksati.mydoc.ui.cleaner.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBinSheet(
    trashItems: List<TrashItem>,
    onRestore: (List<TrashItem>) -> Unit,
    onDeletePermanently: (List<TrashItem>) -> Unit,
    onEmptyTrash: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = LocalIsOled.current

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val isSelectionMode = selectedIds.isNotEmpty()
    val selectedItems = trashItems.filter { it.id in selectedIds }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        OverlaySystemBars()
        BackHandler {
            if (isSelectionMode) {
                selectedIds = emptySet()
            } else {
                onClose()
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                AppTopBar(
                    title = if (isSelectionMode) stringResource(R.string.trash_selected_count, selectedIds.size) else stringResource(R.string.menu_recycle_bin),
                    subtitle = if (isSelectionMode) null
                        else pluralStringResource(R.plurals.trash_meta_count, trashItems.size, trashItems.size),
                    onBack = onClose,
                    isGlass = false,
                    containerColor = colors.background,
                    actions = {
                        if (trashItems.isNotEmpty()) {
                            if (!isSelectionMode) {
                                IconButton(onClick = { showEmptyConfirmDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = stringResource(R.string.trash_empty_bin),
                                        tint = colors.error
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (selectedIds.size == trashItems.size) {
                                            selectedIds = emptySet()
                                        } else {
                                            selectedIds = trashItems.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedIds.size == trashItems.size) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = stringResource(R.string.cd_select_all),
                                        tint = colors.accent
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (isSelectionMode) {
                    Surface(
                        shape = AppRadius.xLargeShape,
                        color = colors.surface,
                        border = BorderStroke(1.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingMedium)
                        ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = AppRadius.cardShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
                        ) {
                            Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.trash_delete))
                        }

                        Button(
                            onClick = {
                                onRestore(selectedItems)
                                selectedIds = emptySet()
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = AppRadius.cardShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.trash_restore))
                        }
                    }
                }
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        if (trashItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(colors.accentAmber.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = colors.accentAmber,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.trash_empty_title),
                    style = typography.titleSmall,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.trash_empty_message),
                    style = typography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = AppSpacing.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "trash_hero") {
                    TrashHeroCard(
                        count = trashItems.size,
                        totalBytes = trashItems.sumOf { it.sizeBytes },
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(trashItems, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds
                    TrashItemCard(
                        item = item,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedIds = if (isSelected) {
                                selectedIds - item.id
                            } else {
                                selectedIds + item.id
                            }
                        },
                        onRestoreSingle = { onRestore(listOf(item)) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showEmptyConfirmDialog) {
        AppAlertDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            shape = AppRadius.modalShape,
            containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
            modifier = Modifier.border(
                BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
                AppRadius.modalShape
            ),
            title = { Text(stringResource(R.string.trash_confirm_empty_title), color = colors.textPrimary) },
            text = {
                OverlaySystemBars()
                Text(stringResource(R.string.trash_confirm_empty_message, trashItems.size), color = colors.textSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyConfirmDialog = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text(stringResource(R.string.trash_meta_empty))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AppAlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = AppRadius.modalShape,
            containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
            modifier = Modifier.border(
                BorderStroke(0.8.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)),
                AppRadius.modalShape
            ),
            title = { Text(stringResource(R.string.trash_confirm_delete_title), color = colors.textPrimary) },
            text = {
                OverlaySystemBars()
                Text(stringResource(R.string.trash_confirm_delete_message, selectedItems.size), color = colors.textSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeletePermanently(selectedItems)
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text(stringResource(R.string.trash_delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        )
    }
    }
}

@Composable
private fun TrashHeroCard(count: Int, totalBytes: Long, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val tone = colors.accentAmber
    val shape = AppRadius.xLargeShape
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface, shape)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(tone.copy(alpha = 0.14f), tone.copy(alpha = 0.02f))
                ),
                shape
            )
            .border(1.dp, tone.copy(alpha = 0.18f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(tone.copy(alpha = 0.16f), AppRadius.insetShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = pluralStringResource(R.plurals.search_result_count, count, count),
                style = typography.titleSmall,
                color = colors.textPrimary
            )
            Text(
                text = stringResource(R.string.trash_hero_caption, formatSize(totalBytes)),
                style = typography.caption,
                color = colors.textSecondary
            )
        }
        Surface(shape = AppRadius.pillShape, color = tone.copy(alpha = 0.14f)) {
            Text(
                text = stringResource(R.string.trash_auto_purge_chip),
                style = typography.label,
                color = tone,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TrashItemCard(
    item: TrashItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onRestoreSingle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val isOled = LocalIsOled.current
    val tone = if (item.isDirectory) colors.accentAmber else colors.textSecondary

    GlassSurface(
        shape = AppRadius.cardShape,
        elevation = 2.dp,
        backgroundColor = if (isSelected) colors.accent.copy(alpha = 0.08f) else null,
        modifier = modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .toggleable(
                value = isSelected,
                role = Role.Checkbox,
                onValueChange = { onToggleSelect() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppRadius.insetShape)
                    .background(tone.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.formattedSize} · ${item.formattedDeletedTime}",
                    style = typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onRestoreSingle) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.cd_restore),
                    tint = colors.accent
                )
            }
        }
    }
}
