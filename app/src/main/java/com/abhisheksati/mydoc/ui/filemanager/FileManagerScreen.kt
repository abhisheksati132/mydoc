package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.data.local.StoragePermissionHelper
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(StoragePermissionHelper.hasStoragePermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermissionHelper.hasStoragePermission(context)
                if (granted != hasPermission) {
                    hasPermission = granted
                    if (granted) {
                        viewModel.refreshVolumes()
                        viewModel.loadDirectory(viewModel.uiState.value.currentDirectory, addToHistory = false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    // Dialog & menu state
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialogForFile by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var singleItemActionTarget by remember { mutableStateOf<StorageItem?>(null) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showExtractConfirmDialogForFile by remember { mutableStateOf<File?>(null) }
    var singleItemCompressTarget by remember { mutableStateOf<File?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }


    // User message toasts
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    // Intercept back presses
    BackHandler(enabled = uiState.isSelectionMode || isSearchActive || uiState.canGoBack || uiState.activeCategory != null) {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else if (isSearchActive) {
            isSearchActive = false
            viewModel.setSearchQuery("")
        } else {
            viewModel.navigateUp()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            if (uiState.isSelectionMode) {
                // Multi-selection Top Bar
                Surface(
                    color = colors.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textPrimary)
                            }
                            Text(
                                text = "${uiState.selectedFiles.size} selected",
                                style = typography.title.copy(fontSize = 16.sp),
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Outlined.SelectAll, contentDescription = "Select All", tint = colors.textPrimary)
                            }
                            IconButton(onClick = { viewModel.copySelected() }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = colors.textPrimary)
                            }
                            IconButton(onClick = { viewModel.cutSelected() }) {
                                Icon(Icons.Outlined.ContentCut, contentDescription = "Move", tint = colors.textPrimary)
                            }
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = colors.error)
                            }
                            IconButton(onClick = { showCompressDialog = true }) {
                                Icon(Icons.Outlined.FolderZip, contentDescription = "Compress to ZIP", tint = colors.textPrimary)
                            }
                            if (uiState.selectedFiles.size == 1) {
                                IconButton(onClick = {
                                    showRenameDialogForFile = uiState.selectedFiles.first()
                                }) {
                                    Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = "Rename", tint = colors.textPrimary)
                                }
                            }
                            IconButton(onClick = {
                                shareFiles(context, uiState.selectedFiles.toList())
                            }) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = colors.textPrimary)
                            }
                        }
                    }
                }

            } else {
                // Normal Top Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.canGoBack) {
                                IconButton(onClick = { viewModel.navigateUp() }) {
                                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier
                                        .padding(start = 6.dp, end = 10.dp)
                                        .size(24.dp)
                                )
                            }
                            Text(
                                text = "Files",
                                style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                color = colors.textPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                                Icon(Icons.Outlined.Search, contentDescription = "Search", tint = colors.textPrimary)
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort", tint = colors.textPrimary)
                                }
                                SortOrderDropdownMenu(
                                    expanded = showSortMenu,
                                    currentSort = uiState.sortOrder,
                                    onDismiss = { showSortMenu = false },
                                    onSortSelected = { viewModel.toggleSort(it) }
                                )
                            }
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (uiState.isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                    contentDescription = "Toggle View",
                                    tint = colors.textPrimary
                                )
                            }
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = colors.textPrimary)
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    modifier = Modifier.background(colors.surface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("New Folder", color = colors.textPrimary) },
                                        leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = colors.accent) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showNewFolderDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("New File", color = colors.textPrimary) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null, tint = colors.accent) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showNewFileDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (uiState.showHiddenFiles) "Hide hidden files" else "Show hidden files",
                                                color = colors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.toggleHiddenFiles()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Refresh", color = colors.textPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.loadDirectory(uiState.currentDirectory, addToHistory = false)
                                            viewModel.refreshVolumes()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Search Field (if toggled)
                    if (isSearchActive) {
                        AppSearchField(
                            query = uiState.searchQuery,
                            onQueryChanged = { viewModel.setSearchQuery(it) },
                            placeholder = "Search current folder...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Fast Categories Chips
                    FileManagerCategoryChips(
                        activeCategory = uiState.activeCategory,
                        onCategorySelected = { category ->
                            if (uiState.activeCategory == category) {
                                viewModel.selectCategory(null)
                            } else {
                                viewModel.selectCategory(category)
                            }
                        }
                    )

                    // Breadcrumb Trail or Category Banner
                    if (uiState.activeCategory != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Category: ${uiState.activeCategory?.displayName}",
                                    style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = colors.accent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${uiState.filteredItems.size} items)",
                                    style = typography.caption.copy(fontSize = 12.sp),
                                    color = colors.textSecondary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.selectCategory(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Exit Category",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            uiState.breadcrumbTrail.forEachIndexed { index, crumb ->
                                val isLast = index == uiState.breadcrumbTrail.size - 1
                                val volumeForCrumb = uiState.storageVolumes.firstOrNull { it.path == crumb.absolutePath }
                                val crumbName = when {
                                    volumeForCrumb != null -> volumeForCrumb.title
                                    crumb.absolutePath == "/storage/emulated/0" -> "Internal Storage"
                                    index == 0 -> "Storage"
                                    else -> crumb.name
                                }

                                Text(
                                    text = crumbName,
                                    style = typography.body.copy(
                                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isLast) colors.accent else colors.textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.navigateToCrumb(crumb) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )

                                if (!isLast) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.divider,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = colors.divider, thickness = 0.8.dp)
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode && uiState.clipboardState == null) {
                FloatingActionButton(
                    onClick = { showNewFolderDialog = true },
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New Folder")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Storage Volume Meter (only when at root of storage volume and not in a category)
                val isAtVolumeRoot = uiState.storageVolumes.any { it.path == uiState.currentDirectory.absolutePath } ||
                        uiState.currentDirectory.absolutePath == "/storage/emulated/0"
                if (uiState.activeCategory == null && isAtVolumeRoot) {
                    StorageVolumeSummary(
                        volumes = uiState.storageVolumes,
                        onVolumeClick = { volume ->
                            viewModel.loadDirectory(volume.file)
                        }
                    )
                }

                // Storage Permission Missing Warning
                if (!hasPermission) {
                    StoragePermissionBanner(
                        onRequestPermission = {
                            StoragePermissionHelper.launchManageStorageSettings(context)
                        }
                    )
                }

                // File List / Grid Content
                if (uiState.isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                } else if (uiState.filteredItems.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AppEmptyState(
                            title = if (uiState.searchQuery.isNotEmpty()) "No matches found"
                                    else if (uiState.activeCategory != null) "No files found"
                                    else "This folder is empty",
                            message = if (uiState.searchQuery.isNotEmpty()) "Try a different search term"
                                      else if (uiState.activeCategory != null) "No ${uiState.activeCategory?.displayName?.lowercase()} files on this device"
                                      else "Tap '+' to create a new folder or file"
                        )
                    }
                } else {
                    if (uiState.isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(
                                items = uiState.filteredItems,
                                key = { it.path }
                            ) { item ->
                                val isSelected = uiState.selectedFiles.contains(item.file)
                                StorageItemGrid(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelection(item.file)
                                        } else {
                                            if (item.isDirectory) {
                                                viewModel.loadDirectory(item.file)
                                            } else if (item.fileType == StorageFileType.ARCHIVE || item.name.endsWith(".zip", ignoreCase = true)) {
                                                singleItemActionTarget = item
                                            } else {
                                                onOpenFile(item.file)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item.file)
                                    },
                                    onMoreClick = {
                                        singleItemActionTarget = item
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(
                                items = uiState.filteredItems,
                                key = { it.path }
                            ) { item ->
                                val isSelected = uiState.selectedFiles.contains(item.file)
                                StorageItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelection(item.file)
                                        } else {
                                            if (item.isDirectory) {
                                                viewModel.loadDirectory(item.file)
                                            } else if (item.fileType == StorageFileType.ARCHIVE || item.name.endsWith(".zip", ignoreCase = true)) {
                                                singleItemActionTarget = item
                                            } else {
                                                onOpenFile(item.file)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.toggleSelection(item.file)
                                    },
                                    onMoreClick = {
                                        singleItemActionTarget = item
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Clipboard Action Bar (Paste / Cancel)
            AnimatedVisibility(
                visible = uiState.clipboardState != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                uiState.clipboardState?.let { clip ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (clip.operation == ClipboardOperation.COPY) Icons.Outlined.ContentCopy else Icons.Outlined.ContentCut,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${clip.items.size} items ready to ${if (clip.operation == ClipboardOperation.COPY) "copy" else "move"}",
                                        style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Navigate to destination folder",
                                        style = typography.caption.copy(fontSize = 11.sp),
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { viewModel.cancelClipboard() }) {
                                    Text("Cancel", color = colors.textSecondary)
                                }
                                Button(
                                    onClick = { viewModel.pasteClipboard() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                                ) {
                                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paste")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                showNewFolderDialog = false
                viewModel.createNewFolder(name)
            }
        )
    }

    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onConfirm = { name ->
                showNewFileDialog = false
                viewModel.createNewFile(name)
            }
        )
    }

    showRenameDialogForFile?.let { file ->
        RenameDialog(
            file = file,
            onDismiss = { showRenameDialogForFile = null },
            onConfirm = { newName ->
                showRenameDialogForFile = null
                viewModel.renameItem(file, newName)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            count = uiState.selectedFiles.size,
            fileNamePreview = uiState.selectedFiles.firstOrNull()?.name,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteSelected()
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
            onDismiss = { showCompressDialog = false },
            onConfirm = { name ->
                showCompressDialog = false
                viewModel.compressSelected(name)
            }
        )
    }

    singleItemCompressTarget?.let { file ->
        CompressDialog(
            defaultName = "${file.nameWithoutExtension}.zip",
            onDismiss = { singleItemCompressTarget = null },
            onConfirm = { name ->
                singleItemCompressTarget = null
                viewModel.compressSingle(file, name)
            }
        )
    }

    showExtractConfirmDialogForFile?.let { file ->
        ExtractConfirmDialog(
            archiveName = file.name,
            onDismiss = { showExtractConfirmDialogForFile = null },
            onConfirm = {
                showExtractConfirmDialogForFile = null
                viewModel.extractArchive(file)
            }
        )
    }

    // Single item context action bottom sheet
    singleItemActionTarget?.let { item ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { singleItemActionTarget = null },
            sheetState = sheetState,
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                            style = typography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.formattedSize,
                            style = typography.caption.copy(fontSize = 11.sp),
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
                    onClick = {
                        singleItemActionTarget = null
                        if (item.isDirectory) {
                            viewModel.loadDirectory(item.file)
                        } else {
                            onOpenFile(item.file)
                        }
                    }
                )

                ItemActionOption(
                    title = "Copy",
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.toggleSelection(item.file)
                        viewModel.copySelected()
                    }
                )

                ItemActionOption(
                    title = "Move (Cut)",
                    icon = Icons.Outlined.ContentCut,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.toggleSelection(item.file)
                        viewModel.cutSelected()
                    }
                )

                ItemActionOption(
                    title = "Rename",
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    onClick = {
                        singleItemActionTarget = null
                        showRenameDialogForFile = item.file
                    }
                )

                ItemActionOption(
                    title = "Delete",
                    icon = Icons.Outlined.Delete,
                    tint = colors.error,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.deleteSingle(item.file)
                    }
                )

                val isZip = item.name.endsWith(".zip", ignoreCase = true) || item.name.endsWith(".jar", ignoreCase = true)
                if (isZip) {
                    ItemActionOption(
                        title = "Extract ZIP Archive",
                        icon = Icons.Outlined.FolderZip,
                        tint = colors.accent,
                        onClick = {
                            singleItemActionTarget = null
                            showExtractConfirmDialogForFile = item.file
                        }
                    )
                }

                ItemActionOption(
                    title = "Compress to ZIP",
                    icon = Icons.Outlined.Archive,
                    onClick = {
                        singleItemActionTarget = null
                        singleItemCompressTarget = item.file
                    }
                )

                ItemActionOption(
                    title = "Details & Checksum",
                    icon = Icons.Outlined.Info,
                    onClick = {
                        singleItemActionTarget = null
                        viewModel.inspectItem(item.file)
                    }
                )

                if (!item.isDirectory) {
                    ItemActionOption(
                        title = "Share",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            singleItemActionTarget = null
                            shareFiles(context, listOf(item.file))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ItemActionOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = LocalAppColors.current.textPrimary,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
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
            style = typography.body.copy(fontSize = 14.sp),
            color = tint
        )
    }
}

@Composable
fun StorageVolumeSummary(
    volumes: List<com.abhisheksati.mydoc.domain.model.StorageVolume>,
    onVolumeClick: (com.abhisheksati.mydoc.domain.model.StorageVolume) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    if (volumes.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        for (volume in volumes) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onVolumeClick(volume) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = volume.title,
                                style = typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                        }
                        Text(
                            text = "${volume.formattedUsed} / ${volume.formattedTotal}",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { volume.usedRatio },
                        color = colors.accent,
                        trackColor = colors.divider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun StoragePermissionBanner(
    onRequestPermission: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.accent.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Full Storage Access Recommended",
                    style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Grant All Files Access to browse and manage files across internal storage and SD cards without restrictions.",
                style = typography.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Storage Access", fontSize = 12.sp)
            }
        }
    }
}

private fun shareFiles(context: Context, files: List<File>) {
    if (files.isEmpty()) return
    try {
        val uris = files.map { file ->
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        context.startActivity(Intent.createChooser(intent, "Share via"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun FileManagerCategoryChips(
    activeCategory: FileCategory?,
    onCategorySelected: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val categories = listOf(
        Triple(FileCategory.DOWNLOADS, Icons.Outlined.Download, Color(0xFF0288D1)),
        Triple(FileCategory.DOCUMENTS, Icons.Outlined.Description, Color(0xFF3F51B5)),
        Triple(FileCategory.IMAGES, Icons.Outlined.Image, Color(0xFF4CAF50)),
        Triple(FileCategory.VIDEOS, Icons.Outlined.Videocam, Color(0xFFE91E63)),
        Triple(FileCategory.AUDIO, Icons.Outlined.AudioFile, Color(0xFF9C27B0)),
        Triple(FileCategory.APKS, Icons.Outlined.Android, Color(0xFF2E7D32)),
        Triple(FileCategory.ARCHIVES, Icons.Outlined.FolderZip, Color(0xFFFFA000)),
        Triple(FileCategory.LARGE_FILES, Icons.Outlined.DataUsage, Color(0xFFFF5722))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((cat, icon, color) in categories) {
            val isSelected = activeCategory == cat
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) color.copy(alpha = 0.2f) else colors.surface,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCategorySelected(cat) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = cat.displayName,
                        tint = if (isSelected) color else color.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.displayName,
                        style = typography.caption.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) colors.textPrimary else colors.textSecondary
                    )
                }
            }
        }
    }
}

