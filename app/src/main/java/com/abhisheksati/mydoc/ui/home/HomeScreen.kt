package com.abhisheksati.mydoc.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.data.local.DocumentSortOrder
import com.abhisheksati.mydoc.data.local.FolderScanner
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.ui.common.EmptyState
import com.abhisheksati.mydoc.ui.theme.Spacing
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

// Supported MIME types for Storage Access Framework document picker
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDocumentClick: (DocumentFile) -> Unit,
    onSettingsClick: () -> Unit,
    onShareDocument: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
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
                    scope.launch {
                        snackbarHostState.showSnackbar("Unable to access the selected document.")
                    }
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
                    snackbarHostState.showSnackbar("Added $count documents from $cleanName")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Unable to access folder: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Sort order menu
                    Box {
                        IconButton(onClick = { isSortMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = "Sort Documents",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Last Opened") },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.LAST_OPENED) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.LAST_OPENED)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)") },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.NAME_ASC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.NAME_ASC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z to A)") },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.NAME_DESC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.NAME_DESC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Largest First)") },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.SIZE_DESC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.SIZE_DESC)
                                    isSortMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Smallest First)") },
                                trailingIcon = {
                                    if (uiState.sortOrder == DocumentSortOrder.SIZE_ASC) {
                                        Icon(Icons.Outlined.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(DocumentSortOrder.SIZE_ASC)
                                    isSortMenuOpen = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = { openFolderLauncher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Scan Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Overflow actions (Clear Recents)
                    Box {
                        IconButton(onClick = { isMoreMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Recents") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    showClearRecentsDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { openDocumentLauncher.launch(SUPPORTED_MIME_TYPES) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.open_document),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                shape = RoundedCornerShape(6.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchAndFilterBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::selectCategory
            )

            Spacer(modifier = Modifier.height(Spacing.small))
            AppDivider()

            if (uiState.documents.isEmpty()) {
                if (uiState.isSearching) {
                    EmptyState(
                        title = stringResource(R.string.no_search_results),
                        subtitle = ""
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.no_documents_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))
                            Text(
                                text = stringResource(R.string.no_documents_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.large))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                                Button(
                                    onClick = { openDocumentLauncher.launch(SUPPORTED_MIME_TYPES) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onBackground
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.open_document),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Button(
                                    onClick = { openFolderLauncher.launch(null) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onBackground
                                    )
                                ) {
                                    Text(
                                        text = "Scan Folder",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.documents,
                        key = { it.uri }
                    ) { document ->
                        DocumentListRow(
                            document = document,
                            onClick = { onDocumentClick(document) },
                            onToggleFavorite = { viewModel.toggleFavorite(document) },
                            onRemoveFromRecents = { viewModel.removeFromRecents(document) },
                            onShare = { onShareDocument(document) },
                            onShowDetails = { selectedDocForDetails = document }
                        )
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

    // Clear Recents Confirmation Dialog
    if (showClearRecentsDialog) {
        AlertDialog(
            onDismissRequest = { showClearRecentsDialog = false },
            title = {
                Text(
                    text = "Clear Recent Documents?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    text = "This will clear your recent document history in mydoc. Your actual files on storage will not be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearRecentsDialog = false
                        viewModel.clearAllRecents()
                        scope.launch {
                            snackbarHostState.showSnackbar("Recent history cleared.")
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearRecentsDialog = false },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}
