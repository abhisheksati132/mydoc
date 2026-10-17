package com.abhisheksati.mydoc.ui.home

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.viewmodel.HomeUiState
import com.abhisheksati.mydoc.viewmodel.HomeViewModel

@Composable
fun DocumentsTabContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSettingsClick: () -> Unit,
    onDocumentClick: (DocumentFile) -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    openFolderLauncher: ActivityResultLauncher<Uri?>,
    openDocumentLauncher: ActivityResultLauncher<Array<String>>,
    onShowClearRecentsDialog: () -> Unit,
    onSelectDocForDetails: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

    var isSortMenuOpen by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        var isSearchActive by remember { mutableStateOf(uiState.searchQuery.isNotEmpty()) }

        Box {
            AppTabHeader(
                title = "Documents",
                onSearchClick = { isSearchActive = !isSearchActive },
                isSearchActive = isSearchActive || uiState.searchQuery.isNotEmpty(),
                searchContent = {
                    AppSearchField(
                        query = uiState.searchQuery,
                        onQueryChanged = viewModel::onSearchQueryChanged,
                        placeholder = "Search documents..."
                    )
                },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onSettingsClick = onSettingsClick,
                moreMenuContent = { onDismiss ->
                    DropdownMenuItem(
                        text = { Text("Sort documents...", style = typography.body) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, tint = colors.accent) },
                        onClick = {
                            onDismiss()
                            isSortMenuOpen = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Scan folder", style = typography.body) },
                        leadingIcon = {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = colors.textSecondary)
                        },
                        onClick = {
                            onDismiss()
                            openFolderLauncher.launch(null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear recents", style = typography.body) },
                        leadingIcon = {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.error)
                        },
                        onClick = {
                            onDismiss()
                            onShowClearRecentsDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", style = typography.body) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Settings, contentDescription = null, tint = colors.textSecondary)
                        },
                        onClick = {
                            onDismiss()
                            onSettingsClick()
                        }
                    )
                },
                bottomContent = {
                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                }
            )

            // Sort Menu
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

        // 2. Documents List or Empty State
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
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
                            onShowDetails = { onSelectDocForDetails(document) }
                        )
                    }
                } else {
                    // 1. "Continue reading" row at very top showing last opened file and progress
                    val continueDoc = uiState.continueReadingDoc
                    if (continueDoc != null && uiState.selectedCategory == DocumentCategory.ALL) {
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
                                onShowDetails = { onSelectDocForDetails(continueDoc) }
                            )
                        }
                    }

                    // 2. Main Documents List
                    val headerTitle = when (uiState.selectedCategory) {
                        DocumentCategory.ALL -> "RECENTS"
                        DocumentCategory.FAVORITES -> "FAVORITES"
                        DocumentCategory.PDF -> "PDF DOCUMENTS"
                        DocumentCategory.WORD -> "WORD DOCUMENTS"
                        DocumentCategory.EXCEL -> "EXCEL SPREADSHEETS"
                        DocumentCategory.PPT -> "POWERPOINT PRESENTATIONS"
                        DocumentCategory.TEXT -> "TEXT DOCUMENTS"
                    }
                    item {
                        AppSectionHeader(title = headerTitle)
                    }
                    items(
                        items = uiState.documents,
                        key = { "doc_${it.uri}" }
                    ) { document ->
                        AppFileRow(
                            document = document,
                            onClick = { onDocumentClick(document) },
                            onToggleFavorite = { viewModel.toggleFavorite(document) },
                            onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                            onShare = { onShareDocument(document) },
                            onShowDetails = { onSelectDocForDetails(document) }
                        )
                    }

                    // 3. Favorites Section (if on ALL and has favorites)
                    if (uiState.selectedCategory == DocumentCategory.ALL && uiState.favorites.isNotEmpty()) {
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
                                onShowDetails = { onSelectDocForDetails(document) }
                            )
                        }
                    }
                }
            }
        }
    }
}
