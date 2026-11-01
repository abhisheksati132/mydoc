package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.abhisheksati.mydoc.data.local.PinnedFoldersManager
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.ui.appmanager.AppManagerSheet
import com.abhisheksati.mydoc.ui.cleaner.StorageCleanerSheet
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import java.io.File

/**
 * Encapsulated overlays, dialogs, and contextual bottom sheets for FileManagerScreen.
 */
@Composable
fun FileManagerOverlays(
    viewModel: FileManagerViewModel,
    storageViewModel: StorageViewModel,
    uiState: FileManagerUiState,
    pinnedFoldersManager: PinnedFoldersManager,
    showNewFolderDialog: Boolean,
    onDismissNewFolder: () -> Unit,
    showNewFileDialog: Boolean,
    onDismissNewFile: () -> Unit,
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
    showStorageCleanerSheet: Boolean,
    onDismissStorageCleaner: () -> Unit,
    showAppManagerSheet: Boolean,
    onDismissAppManager: () -> Unit,
    singleItemActionTarget: StorageItem?,
    onDismissSingleItemAction: () -> Unit,
    onRequestRename: (File) -> Unit,
    onOpenFile: (File) -> Unit
) {
    val context = LocalContext.current

    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = onDismissNewFolder,
            onConfirm = { name ->
                onDismissNewFolder()
                viewModel.createNewFolder(name)
            }
        )
    }

    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = onDismissNewFile,
            onConfirm = { name ->
                onDismissNewFile()
                viewModel.createNewFile(name)
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
        val defaultName = if (uiState.selectedFiles.size == 1) {
            "${uiState.selectedFiles.first().nameWithoutExtension}.zip"
        } else "Archive.zip"
        CompressDialog(
            defaultName = defaultName,
            onDismiss = onDismissCompress,
            onConfirm = { name ->
                onDismissCompress()
                viewModel.compressSelected(name)
            }
        )
    }

    singleItemCompressTarget?.let { file ->
        CompressDialog(
            defaultName = "${file.nameWithoutExtension}.zip",
            onDismiss = onDismissSingleCompress,
            onConfirm = { name ->
                onDismissSingleCompress()
                viewModel.compressSingle(file, name)
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

    if (showStorageCleanerSheet) {
        StorageCleanerSheet(
            rootDir = android.os.Environment.getExternalStorageDirectory(),
            onDismiss = {
                onDismissStorageCleaner()
                viewModel.refreshVolumes()
                storageViewModel.refresh()
                if (uiState.isBrowsingStorage) {
                    viewModel.loadDirectory(uiState.currentDirectory, addToHistory = false)
                }
            },
            onFileClick = { file ->
                onDismissStorageCleaner()
                onOpenFile(file)
            }
        )
    }

    if (showAppManagerSheet) {
        AppManagerSheet(
            onDismiss = {
                onDismissAppManager()
                viewModel.refreshVolumes()
                storageViewModel.refresh()
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
                viewModel.toggleSelection(item.file)
                viewModel.copySelected()
            },
            onMove = {
                onDismissSingleItemAction()
                viewModel.toggleSelection(item.file)
                viewModel.cutSelected()
            },
            onRename = {
                onDismissSingleItemAction()
                onRequestRename(item.file)
            },
            onDelete = {
                onDismissSingleItemAction()
                viewModel.moveToTrashSingle(item.file)
            },
            onTogglePin = {
                onDismissSingleItemAction()
                pinnedFoldersManager.togglePin(item.file)
            },
            onExtractZip = {
                onDismissSingleItemAction()
                viewModel.extractArchive(item.file)
            },
            onCompressToZip = {
                onDismissSingleItemAction()
                viewModel.compressSingle(item.file, "${item.file.nameWithoutExtension}.zip")
            },
            onDetails = {
                onDismissSingleItemAction()
                viewModel.inspectItem(item.file)
            },
            onShare = {
                onDismissSingleItemAction()
                shareFiles(context, listOf(item.file))
            }
        )
    }
}
