package com.abhisheksati.mydoc.ui.filemanager

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.viewmodel.FileManagerUiState
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import java.io.File

@Composable
fun FileManagerTopBar(
    uiState: FileManagerUiState,
    viewModel: FileManagerViewModel,
    displayedItemsCount: Int,
    documentFilter: DocumentFilter,
    isHubMode: Boolean,
    isSearchActive: Boolean,
    showSortMenu: Boolean,
    onToggleSearch: () -> Unit,
    onToggleSortMenu: (Boolean) -> Unit,
    onDocumentFilterChange: (DocumentFilter) -> Unit,
    onShowNewFolderDialog: () -> Unit,
    onShowNewFileDialog: () -> Unit,
    onShowDeleteConfirmDialog: () -> Unit,
    onShowCompressDialog: () -> Unit,
    onRenameSingleSelected: (File) -> Unit,
    onToggleTheme: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current

    AnimatedContent(
        targetState = uiState.isSelectionMode,
        transitionSpec = {
            (slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow))).togetherWith(
                slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut()
            )
        },
        label = "FileManagerTopBar",
        modifier = modifier
    ) { isSelection ->
        if (isSelection) {
            FileManagerSelectionTopBar(
                selectedCount = uiState.selectedFiles.size,
                onClearSelection = { viewModel.clearSelection() },
                onShare = { shareFiles(context, uiState.selectedFiles.toList()) },
                onCut = { viewModel.cutSelected() },
                onDelete = onShowDeleteConfirmDialog,
                onSelectAll = { viewModel.selectAll() },
                onCopy = { viewModel.copySelected() },
                onCompress = onShowCompressDialog,
                onRename = if (uiState.selectedFiles.size == 1) {
                    { onRenameSingleSelected(uiState.selectedFiles.first()) }
                } else null
            )
        } else {
            Box {
                AppTabHeader(
                    title = when {
                        uiState.activeCategory != null -> uiState.activeCategory!!.displayName
                        uiState.isBrowsingStorage -> {
                            val isAtRoot = uiState.storageVolumes.any { it.path == uiState.currentDirectory.absolutePath } ||
                                    uiState.currentDirectory.absolutePath == "/storage/emulated/0"
                            if (isAtRoot) "Internal Storage" else uiState.currentDirectory.name
                        }
                        else -> "Files"
                    },
                    navigationIcon = if (uiState.activeCategory != null || uiState.isBrowsingStorage) {
                        {
                            IconButton(onClick = {
                                if (uiState.activeCategory != null) {
                                    viewModel.selectCategory(null)
                                } else {
                                    viewModel.navigateUp()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }
                        }
                    } else null,
                    onSearchClick = onToggleSearch,
                    isSearchActive = isSearchActive,
                    searchContent = {
                        AppSearchField(
                            query = uiState.searchQuery,
                            onQueryChanged = { viewModel.setSearchQuery(it) },
                            placeholder = "Search files..."
                        )
                    },
                    onToggleTheme = onToggleTheme,
                    onSettingsClick = onSettingsClick,
                    moreMenuContent = { onDismiss ->
                        DropdownMenuItem(
                            text = { Text(if (uiState.isGridView) "Switch to List View" else "Switch to Grid View", style = typography.body, color = colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                    contentDescription = null,
                                    tint = colors.accent
                                )
                            },
                            onClick = {
                                onDismiss()
                                viewModel.toggleViewMode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort files...", style = typography.body, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                onDismiss()
                                onToggleSortMenu(true)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (uiState.showHiddenFiles) "Hide hidden files" else "Show hidden files",
                                    style = typography.body,
                                    color = colors.textPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.showHiddenFiles) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = colors.accent
                                )
                            },
                            onClick = {
                                onDismiss()
                                viewModel.toggleHiddenFiles()
                            }
                        )

                        HorizontalDivider(color = colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("New Folder", style = typography.body, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                onDismiss()
                                onShowNewFolderDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New File", style = typography.body, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                onDismiss()
                                onShowNewFileDialog()
                            }
                        )

                        HorizontalDivider(color = colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text(if (uiState.trashItems.isNotEmpty()) "Recycle Bin (${uiState.trashItems.size})" else "Recycle Bin", style = typography.body, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                onDismiss()
                                viewModel.openTrash()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Refresh", style = typography.body, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null, tint = colors.textSecondary) },
                            onClick = {
                                onDismiss()
                                viewModel.loadDirectory(uiState.currentDirectory, addToHistory = false)
                                viewModel.refreshVolumes()
                            }
                        )
                    },
                    bottomContent = if (!isHubMode) {
                        {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Fast Categories Chips
                                FileManagerCategoryChips(
                                    activeCategory = uiState.activeCategory,
                                    onCategorySelected = { category ->
                                        onDocumentFilterChange(DocumentFilter.ALL)
                                        if (uiState.activeCategory == category) {
                                            viewModel.selectCategory(null)
                                        } else {
                                            viewModel.selectCategory(category)
                                        }
                                    }
                                )

                                // Breadcrumb Trail or Category Banner
                                FileManagerBreadcrumbBar(
                                    activeCategory = uiState.activeCategory,
                                    displayedItemsCount = displayedItemsCount,
                                    onClearCategory = {
                                        onDocumentFilterChange(DocumentFilter.ALL)
                                        viewModel.selectCategory(null)
                                    },
                                    documentFilter = documentFilter,
                                    onDocumentFilterSelected = onDocumentFilterChange,
                                    breadcrumbTrail = uiState.breadcrumbTrail,
                                    storageVolumes = uiState.storageVolumes,
                                    onNavigateToCrumb = { viewModel.navigateToCrumb(it) }
                                )
                            }
                        }
                    } else null
                )

                SortOrderDropdownMenu(
                    expanded = showSortMenu,
                    currentSort = uiState.sortOrder,
                    onDismiss = { onToggleSortMenu(false) },
                    onSortSelected = { viewModel.toggleSort(it) }
                )
            }
        }
    }
}
