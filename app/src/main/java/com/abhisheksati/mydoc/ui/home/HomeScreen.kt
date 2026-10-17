package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppToolbar
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

val SUPPORTED_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-powerpoint",
    "text/plain",
    "text/csv"
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDocumentClick: (DocumentFile) -> Unit,
    onSettingsClick: () -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp

    var isSortMenuOpen by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var showClearRecentsDialog by remember { mutableStateOf(false) }
    var selectedDocForDetails by remember { mutableStateOf<DocumentFile?>(null) }

    // SAF Document Picker launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onDocumentOpened(uri) { result ->
                result.onSuccess { doc ->
                    onDocumentClick(doc)
                }.onFailure {
                    Toast.makeText(context, "Unable to access the selected document", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Folder Picker launcher
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null && app != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val segment = uri.lastPathSegment ?: "Folder"
                val cleanName = segment.substringAfterLast(':').ifEmpty { segment }
                app.settingsRepository.setGrantedFolder(uri.toString(), cleanName)
                scope.launch {
                    val count = FolderScanner.scanTree(context, uri, app.documentRepository)
                    Toast.makeText(context, "Added $count documents from $cleanName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to access folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbar(
                title = stringResource(R.string.app_name),
                actions = {
                    // Sort order dropdown
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isSortMenuOpen = true
                            },
                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = "Sort documents",
                                tint = colors.textSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Last opened", style = typography.body) },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.LAST_OPENED) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.LAST_OPENED)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)", style = typography.body) },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.NAME_ASC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.NAME_ASC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z to A)", style = typography.body) },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.NAME_DESC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.NAME_DESC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (largest first)", style = typography.body) },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.SIZE_DESC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.SIZE_DESC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (smallest first)", style = typography.body) },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.SIZE_ASC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.accent)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.SIZE_ASC)
                                    isSortMenuOpen = false
                                }
                            )
                        }
                    }

                    // Folder scan button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            openFolderLauncher.launch(null)
                        },
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Scan folder",
                            tint = colors.textSecondary
                        )
                    }

                    // Settings button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSettingsClick()
                        },
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colors.textSecondary
                        )
                    }

                    // Clear recents menu
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isMoreMenuOpen = true
                            },
                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options",
                                tint = colors.textSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear recents", style = typography.body) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.error)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    showClearRecentsDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Lower-half one-hand reach control surface
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .navigationBarsPadding()
            ) {
                AppDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingSmall)
                ) {
                    // Search Field (lower half reach)
                    AppSearchField(
                        query = uiState.searchQuery,
                        onQueryChanged = viewModel::onSearchQueryChanged,
                        placeholder = "Search documents...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

                    // Horizontal Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                    ) {
                        AppFilterChip(
                            label = "All",
                            isSelected = uiState.selectedCategory == DocumentCategory.ALL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.ALL)
                            }
                        )
                        AppFilterChip(
                            label = "PDF",
                            isSelected = uiState.selectedCategory == DocumentCategory.PDF,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.PDF)
                            }
                        )
                        AppFilterChip(
                            label = "Word",
                            isSelected = uiState.selectedCategory == DocumentCategory.WORD,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.WORD)
                            }
                        )
                        AppFilterChip(
                            label = "Excel",
                            isSelected = uiState.selectedCategory == DocumentCategory.EXCEL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.EXCEL)
                            }
                        )
                        AppFilterChip(
                            label = "PPT",
                            isSelected = uiState.selectedCategory == DocumentCategory.PPT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.PPT)
                            }
                        )
                        AppFilterChip(
                            label = "Text",
                            isSelected = uiState.selectedCategory == DocumentCategory.TEXT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCategory(DocumentCategory.TEXT)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

                    // Primary Action Button (Open Document, >=48dp touch target)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppSpacing.minTouchTarget)
                            .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                            .background(colors.accent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                openDocumentLauncher.launch(SUPPORTED_MIME_TYPES)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileOpen,
                                contentDescription = null,
                                tint = colors.background,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                            Text(
                                text = stringResource(R.string.open_document),
                                style = typography.body,
                                color = colors.background
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.documents.isEmpty()) {
                if (uiState.isSearching) {
                    AppEmptyState(
                        title = "No documents matched your search.",
                        message = "Try another search term or clear the filter."
                    )
                } else {
                    AppEmptyState(
                        title = "No documents found.",
                        message = "Open a file or scan a folder to begin.",
                        actionLabel = "Open document",
                        onAction = { openDocumentLauncher.launch(SUPPORTED_MIME_TYPES) }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.isSearching) {
                        item {
                            AppSectionHeader(title = "SEARCH RESULTS")
                        }
                        items(
                            items = uiState.documents,
                            key = { "search_${it.uri}" }
                        ) { document ->
                            AppFileRow(
                                document = document,
                                onClick = { onDocumentClick(document) },
                                onToggleFavorite = { viewModel.toggleFavorite(document) },
                                onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                                onShare = { onShareDocument(document) },
                                onShowDetails = { selectedDocForDetails = document }
                            )
                            AppDivider()
                        }
                    } else {
                        // 1. "Continue reading" row at very top showing last opened file and progress
                        val continueDoc = uiState.continueReadingDoc
                        if (continueDoc != null) {
                            item {
                                AppSectionHeader(title = "CONTINUE READING")
                            }
                            item(key = "continue_${continueDoc.uri}") {
                                AppFileRow(
                                    document = continueDoc,
                                    showContinueReading = true,
                                    onClick = { onDocumentClick(continueDoc) },
                                    onToggleFavorite = { viewModel.toggleFavorite(continueDoc) },
                                    onRemoveFromRecents = { viewModel.removeFromRecents(continueDoc) },
                                    onShare = { onShareDocument(continueDoc) },
                                    onShowDetails = { selectedDocForDetails = continueDoc }
                                )
                                AppDivider()
                            }
                        }

                        // 2. Recents plain list
                        if (uiState.recents.isNotEmpty()) {
                            item {
                                AppSectionHeader(title = "RECENTS")
                            }
                            items(
                                items = uiState.recents,
                                key = { "recent_${it.uri}" }
                            ) { document ->
                                AppFileRow(
                                    document = document,
                                    onClick = { onDocumentClick(document) },
                                    onToggleFavorite = { viewModel.toggleFavorite(document) },
                                    onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                                    onShare = { onShareDocument(document) },
                                    onShowDetails = { selectedDocForDetails = document }
                                )
                                AppDivider()
                            }
                        }

                        // 3. Favorites plain list
                        if (uiState.favorites.isNotEmpty()) {
                            item {
                                AppSectionHeader(title = "FAVORITES")
                            }
                            items(
                                items = uiState.favorites,
                                key = { "fav_${it.uri}" }
                            ) { document ->
                                AppFileRow(
                                    document = document,
                                    onClick = { onDocumentClick(document) },
                                    onToggleFavorite = { viewModel.toggleFavorite(document) },
                                    onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                                    onShare = { onShareDocument(document) },
                                    onShowDetails = { selectedDocForDetails = document }
                                )
                                AppDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    // Document Details Dialog
    if (selectedDocForDetails != null) {
        DocumentDetailsDialog(
            document = selectedDocForDetails,
            onDismiss = { selectedDocForDetails = null },
            onOpen = { doc ->
                selectedDocForDetails = null
                onDocumentClick(doc)
            },
            onShare = { doc ->
                onShareDocument(doc)
            },
            onRemove = { doc ->
                selectedDocForDetails = null
                viewModel.removeFromRecents(doc)
            }
        )
    }

    // Clear Recents Confirmation Dialog (Destructive action confirms, no undo snackbar)
    AppDialog(
        isOpen = showClearRecentsDialog,
        title = "Clear recent history",
        message = "Recent document history will be cleared. Files on storage will remain intact.",
        confirmLabel = "Clear all",
        dismissLabel = "Cancel",
        onConfirm = {
            showClearRecentsDialog = false
            viewModel.clearAllRecents()
            Toast.makeText(context, "Recent history cleared", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { showClearRecentsDialog = false }
    )
}
