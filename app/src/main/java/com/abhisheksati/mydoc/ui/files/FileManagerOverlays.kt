package com.abhisheksati.mydoc.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.abhisheksati.mydoc.data.local.PinnedFoldersManager
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import java.io.File

/**
 * Encapsulated overlays, dialogs, and contextual bottom sheets for FileManagerScreen.
 */
@Composable
fun FileManagerOverlays(
    viewModel: FileManagerViewModel,
    uiState: FileManagerUiState,
    pinnedFoldersManager: PinnedFoldersManager,
    showNewFolderDialog: Boolean,
    onDismissNewFolder: () -> Unit,
    showRenameDialogForFile: File?,
    onDismissRename: () -> Unit,
    showDeleteConfirmDialog: Boolean,
    onDismissDeleteConfirm: () -> Unit,
    showCompressDialog: Boolean,
    onDismissCompress: () -> Unit,
    singleItemCompressTarget: File?,
    onDismissSingleCompress: () -> Unit,
    showExtractConfirmDialogForFile: File?,
    onDismissExtractConfirm: () -> Unit,
    singleItemActionTarget: StorageItem?,
    onDismissSingleItemAction: () -> Unit,
    onRequestRename: (File) -> Unit,
    onOpenFile: (File) -> Unit,
    onOpenInTools: ((File) -> Unit)? = null,
    onRequestPreview: ((StorageItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var singleItemPendingDelete by remember { mutableStateOf<File?>(null) }
    var archiveCreateTarget by remember { mutableStateOf<File?>(null) }
    var archiveJobsOpen by remember { mutableStateOf(false) }
    if (archiveJobsOpen) ArchiveJobsSheet { archiveJobsOpen = false; viewModel.refreshCurrentSource() }
    archiveCreateTarget?.let { file ->
        ArchiveCreateSheet(listOf(file), file.parentFile ?: uiState.currentDirectory,
            onDismiss = { archiveCreateTarget = null }, onQueued = { archiveCreateTarget = null; archiveJobsOpen = true })
    }

    singleItemPendingDelete?.let { file ->
        DeleteConfirmationDialog(
            count = 1,
            fileNamePreview = file.name,
            onDismiss = { singleItemPendingDelete = null },
            onConfirm = {
                val target = singleItemPendingDelete
                singleItemPendingDelete = null
                target?.let { viewModel.moveToTrashSingle(it) }
            }
        )
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = onDismissNewFolder,
            onConfirm = { name ->
                onDismissNewFolder()
                viewModel.createNewFolder(name)
            }
        )
    }

    showRenameDialogForFile?.let { file ->
        RenameDialog(
            file = file,
            onDismiss = onDismissRename,
            onConfirm = { newName ->
                onDismissRename()
                viewModel.renameItem(file, newName)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            count = uiState.selectedFiles.size,
            fileNamePreview = uiState.selectedFiles.firstOrNull()?.name,
            onDismiss = onDismissDeleteConfirm,
            onConfirm = {
                onDismissDeleteConfirm()
                viewModel.moveToTrashSelected()
            }
        )
    }

    uiState.selectedProperties?.let { props ->
        FilePropertiesDialog(
            properties = props,
            checksum = uiState.calculatedChecksum,
            onDismiss = { viewModel.dismissPropertiesDialog() }
        )
    }

    if (showCompressDialog) {
        ArchiveCreateSheet(
            sources = uiState.selectedFiles.toList(), parent = uiState.currentDirectory,
            onDismiss = onDismissCompress,
            onQueued = {
                onDismissCompress()
                archiveJobsOpen = true
            }
        )
    }

    singleItemCompressTarget?.let { file ->
        ArchiveCreateSheet(
            sources = listOf(file), parent = file.parentFile ?: uiState.currentDirectory,
            onDismiss = onDismissSingleCompress,
            onQueued = {
                onDismissSingleCompress()
                archiveJobsOpen = true
            }
        )
    }

    showExtractConfirmDialogForFile?.let { file ->
        ExtractConfirmDialog(
            archiveName = file.name,
            onDismiss = onDismissExtractConfirm,
            onConfirm = {
                onDismissExtractConfirm()
                viewModel.extractArchive(file)
            }
        )
    }

    if (uiState.isTrashOpen) {
        TrashBinSheet(
            trashItems = uiState.trashItems,
            onRestore = { items ->
                viewModel.restoreTrashItems(items)
            },
            onDeletePermanently = { items ->
                viewModel.permanentlyDeleteTrashItems(items)
            },
            onEmptyTrash = {
                viewModel.emptyTrash()
            },
            onClose = {
                viewModel.closeTrash()
            }
        )
    }

    // Single item context action bottom sheet
    singleItemActionTarget?.let { item ->
        SingleItemActionBottomSheet(
            item = item,
            isPinned = item.isDirectory && pinnedFoldersManager.isPinned(item.file),
            onDismissRequest = onDismissSingleItemAction,
            onPreview = onRequestPreview?.let { preview ->
                {
                    onDismissSingleItemAction()
                    preview(item)
                }
            },
            onOpen = {
                onDismissSingleItemAction()
                if (item.isDirectory) {
                    viewModel.loadDirectory(item.file)
                } else {
                    onOpenFile(item.file)
                }
            },
            onCopy = {
                onDismissSingleItemAction()
                viewModel.copyItem(item.file)
            },
            onMove = {
                onDismissSingleItemAction()
                viewModel.cutItem(item.file)
            },
            onRename = {
                onDismissSingleItemAction()
                onRequestRename(item.file)
            },
            onDelete = {
                val targetFile = item.file
                onDismissSingleItemAction()
                singleItemPendingDelete = targetFile
            },
            onTogglePin = {
                onDismissSingleItemAction()
                pinnedFoldersManager.togglePin(item.file)
            },
            onExtractZip = {
                onDismissSingleItemAction()
                onOpenFile(item.file)
            },
            onCompressToZip = {
                onDismissSingleItemAction()
                archiveCreateTarget = item.file
            },
            onDetails = {
                onDismissSingleItemAction()
                viewModel.inspectItem(item.file)
            },
            onShare = {
                onDismissSingleItemAction()
                shareFiles(context, listOf(item.file))
            },
            onOpenInTools = onOpenInTools?.let { callback ->
                {
                    onDismissSingleItemAction()
                    callback(item.file)
                }
            }
        )
    }
}
