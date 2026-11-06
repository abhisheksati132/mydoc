package com.abhisheksati.mydoc.ui.files

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.ui.storage.StorageViewModel
import com.abhisheksati.mydoc.viewmodel.*
import dev.chrisbanes.haze.HazeState
import com.abhisheksati.mydoc.designsystem.components.GlassMotionState
import java.io.File

@Composable
fun FilesScreen(
    viewModel: FileManagerViewModel,
    homeViewModel: HomeViewModel,
    storageViewModel: StorageViewModel,
    onOpenFile: (File, List<File>?) -> Unit,
    onLibraryItemClick: (LibraryItem) -> Unit,
    onLibraryItemDetails: (LibraryItem) -> Unit,
    onSettingsClick: () -> Unit,
    onSpotlightSearch: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    onOpenInTools: ((File) -> Unit)? = null,
    hazeState: HazeState? = null,
    glassMotion: GlassMotionState? = null
) {
    val app = LocalContext.current.applicationContext as MyDocApp
    val workspace: ExplorerWorkspaceViewModel = viewModel(key = "explorer-workspace", factory = remember(viewModel) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                ExplorerWorkspaceViewModel(viewModel, app.storageRepository, app.settingsRepository, extras.createSavedStateHandle()) as T
        }
    })
    val tabs by workspace.tabs.collectAsState()
    val active by workspace.active.collectAsState()
    val secondary by workspace.secondary.collectAsState()
    val focused by workspace.focused.collectAsState()
    val savedStates = rememberSaveableStateHolder()
    var showTransfers by remember { mutableStateOf(false) }
    var comparisonFolders by remember { mutableStateOf<List<File>?>(null) }
    val transferHistory by viewModel.explorerClipboard.transfers.state.collectAsState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        Column {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                        tabs.forEach { tab ->
                            val state by tab.browser.uiState.collectAsState()
                            val label = when {
                                !state.isBrowsingStorage -> stringResource(R.string.files_title_library)
                                state.storageVolumes.any { it.isPrimary && it.path == state.currentDirectory.absolutePath } -> stringResource(R.string.files_title_internal)
                                else -> state.currentDirectory.name.ifBlank { "/" }
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                FilterChip(selected = active == tab.id, onClick = { workspace.select(tab.id) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp)) })
                                if (tabs.size > 1) IconButton(onClick = { savedStates.removeState(tab.id); workspace.close(tab.id) }, enabled = state.activeOperation == null) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.explorer_close_tab, label))
                                }
                            }
                        }
                    }
                    IconButton(onClick = {
                        comparisonFolders = tabs.mapNotNull { tab ->
                            tab.browser.uiState.value.takeIf { it.isBrowsingStorage && it.activeCategory == null }?.currentDirectory
                        }
                    }) { Icon(Icons.Outlined.CompareArrows, contentDescription = stringResource(R.string.compare_title)) }
                    IconButton(onClick = { showTransfers = true }) {
                        BadgedBox(badge = { if (transferHistory.any { it.running }) Badge { Text("1") } }) {
                            Icon(Icons.Outlined.SwapHoriz, contentDescription = stringResource(R.string.transfers_title))
                        }
                    }
                    IconButton(onClick = { workspace.add() }, enabled = tabs.size < 6) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.explorer_new_tab)) }
                    if (wide) IconButton(onClick = { workspace.toggleSplit() }) {
                        Icon(Icons.Outlined.VerticalSplit, contentDescription = stringResource(if (secondary == null) R.string.explorer_split else R.string.explorer_single_pane))
                    }
                }
            }
            Row(Modifier.weight(1f)) {
                val visible = listOfNotNull(tabs.firstOrNull { it.id == active }, if (wide) tabs.firstOrNull { it.id == secondary && it.id != active } else null)
                visible.forEachIndexed { index, tab ->
                    if (index > 0) VerticalDivider()
                    Column(Modifier.weight(1f).pointerInput(tab.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            workspace.focus(tab.id)
                        }
                    }) {
                        if (visible.size > 1) TextButton(onClick = { workspace.focus(tab.id) }, modifier = Modifier.semantics { selected = focused == tab.id }) {
                            Text(stringResource(if (index == 0) R.string.explorer_left_pane else R.string.explorer_right_pane))
                        }
                        savedStates.SaveableStateProvider(tab.id) {
                            FilesPaneScreen(tab.browser, homeViewModel, storageViewModel, onOpenFile, onLibraryItemClick,
                                onLibraryItemDetails, onSettingsClick, onSpotlightSearch, isActive = isActive && (focused == tab.id || !wide),
                                onOpenInTools = onOpenInTools, hazeState = hazeState, glassMotion = glassMotion,
                                workspaceInsets = true, onShowTransfers = { showTransfers = true })
                        }
                    }
                }
            }
        }
    }
    comparisonFolders?.let { folders -> ExplorerComparisonSheet(folders) { comparisonFolders = null } }
    if (showTransfers) ExplorerTransfersSheet(viewModel.explorerClipboard.transfers) { showTransfers = false }

}
