package com.abhisheksati.mydoc.ui.cleaner

import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog

import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue

import kotlinx.coroutines.withContext
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.util.UserMessages
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import androidx.compose.foundation.layout.PaddingValues
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.showMessage
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.tools.cleaner.StorageCleanScanResult
import com.abhisheksati.mydoc.tools.cleaner.StorageCleanerManager
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCleanerScreen(
    rootDir: File,
    onDismiss: () -> Unit,
    onFileClick: (File) -> Unit = {}
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val coroutineScope = rememberCoroutineScope()
    val cleanerManager = remember { StorageCleanerManager() }
    val app = LocalContext.current.applicationContext as MyDocApp
    val resources = LocalResources.current
    // The sheet only ever scans one root; naming it stops the reader assuming it covers a card
    // they have mounted.
    val rootLabel = if (rootDir.absolutePath == android.os.Environment.getExternalStorageDirectory().absolutePath) {
        stringResource(R.string.cleaner_root_internal)
    } else {
        rootDir.name.ifBlank { rootDir.absolutePath }
    }

    var isScanning by remember { mutableStateOf(true) }
    var scanAttempt by remember { mutableIntStateOf(0) }
    var scanFailed by remember { mutableStateOf(false) }
    var excludedPaths by remember { mutableStateOf(emptySet<String>()) }
    var scannedCount by remember { mutableIntStateOf(0) }
    var currentScannedName by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<StorageCleanScanResult?>(null) }
    var isCleaning by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var cleanedBytes by remember { mutableStateOf<Long?>(null) }
    var cleanedCount by remember { mutableIntStateOf(0) }

    // Nothing is selected by default: the user has to opt in to every category before
    // anything leaves its original folder, even for a reversible move to Trash.
    var cleanTempFiles by remember { mutableStateOf(false) }
    var cleanApks by remember { mutableStateOf(false) }
    var cleanDuplicates by remember { mutableStateOf(false) }
    var cleanEmptyFolders by remember { mutableStateOf(false) }

    var duplicateKeepers by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(mapOf<String,String>()) }
    var showDuplicateReview by remember { mutableStateOf(false) }

    // Expanded states
    var expandDuplicates by remember { mutableStateOf(false) }
    var expandLargeFiles by remember { mutableStateOf(false) }

    // Start scanning on launch
    LaunchedEffect(rootDir, scanAttempt) {
        isScanning = true
        scanFailed = false
        scanResult = null
        scannedCount = 0
        currentScannedName = ""
        cleanedCount = 0
        cleanedBytes = null
        cleanTempFiles = false
        cleanApks = false
        cleanDuplicates = false
        cleanEmptyFolders = false
        duplicateKeepers = emptyMap()
        excludedPaths = emptySet()
        try {
            scanResult = cleanerManager.scanDirectory(rootDir) { count, name ->
                scannedCount = count
                currentScannedName = name
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            scanFailed = true
        } finally {
            isScanning = false
        }
    }

    val isOled = LocalIsOled.current

    val res = scanResult ?: StorageCleanScanResult()
    // Only the extra copies go; the kept original of every duplicate group stays.
    val candidateFiles = buildList {
        if (cleanTempFiles) addAll(res.tempFiles)
        if (cleanApks) addAll(res.apkFiles)
        if (cleanDuplicates) addAll(duplicateCopies(res.duplicateGroups, duplicateKeepers))
    }.distinctBy { it.absolutePath }.filterNot { it.absolutePath in duplicateKeepers.values }
    val candidateEmptyDirs = if (cleanEmptyFolders) res.emptyFolders else emptyList()
    val selectedFiles = candidateFiles.filterNot { it.absolutePath in excludedPaths }
    val selectedEmptyDirs = candidateEmptyDirs.filterNot { it.absolutePath in excludedPaths }
    // One stat per selected file: it used to run on every recomposition of the sheet, on the
    // main thread, while the reader was watching the checkbox respond.
    val selectedBytesToFree by produceState(0L, selectedFiles) {
        value = withContext(Dispatchers.IO) { selectedFiles.sumOf { it.length() } }
    }
    val hasSelection = selectedFiles.isNotEmpty() || selectedEmptyDirs.isNotEmpty()

    if(showDuplicateReview) DuplicateReview(res.duplicateGroups,duplicateKeepers,
        onKeep={checksum,path ->
            duplicateKeepers=if(path==null) duplicateKeepers-checksum else duplicateKeepers+(checksum to path)
            cleanDuplicates=duplicateKeepers.isNotEmpty()
        },onPreview={file ->showDuplicateReview=false;onFileClick(file)},onDismiss={showDuplicateReview=false})

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            com.abhisheksati.mydoc.designsystem.components.AppTopBar(
                title = stringResource(R.string.cleaner_title),
                subtitle = stringResource(R.string.cleaner_subtitle, rootLabel),
                onBack = onDismiss,
                actions = {
                    if (!isScanning && !isCleaning) TextButton(onClick = { scanAttempt++ }) {
                        Text(stringResource(R.string.cleaner_rescan))
                    }
                },
                contentInsets = WindowInsets(0, 0, 0, 0),
                isGlass = false,
                containerColor = colors.background
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (scanFailed) {
                Text(stringResource(R.string.cleaner_scan_failed), color = colors.inkMuted)
            } else if (isScanning) {
                // Scanning animation state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AppProgressIndicator(
                            size = 56.dp,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.cleaner_scanning),
                            style = typography.bodyStrong,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.cleaner_files_analyzed, scannedCount),
                            style = typography.label,
                            color = colors.textSecondary
                        )
                        if (currentScannedName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentScannedName,
                                style = typography.micro,
                                color = colors.textSecondary.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            } else if (cleanedBytes != null) {
                // Cleaned success state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.cleaner_moved_to_trash),
                            style = typography.heading,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.cleaner_moved_summary,
                                cleanedCount,
                                formatSize(cleanedBytes ?: 0L),
                                stringResource(R.string.cleaner_empty_trash_hint)
                            ),
                            style = typography.body,
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = AppRadius.cardShape
                        ) {
                            Text(stringResource(R.string.action_done), color = Color.White)
                        }
                    }
                }
            } else {
                // Results list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = AppSpacing.paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(stringResource(R.string.cleaner_review_explanation),
                            style = typography.caption, color = colors.inkMuted)
                    }
                    // Summary card
                    item {
                        Surface(
                            color = colors.accent.copy(alpha = 0.1f),
                            shape = AppRadius.largeShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.cleaner_reclaimable),
                                    style = typography.overline,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatSize(res.totalReclaimableBytes),
                                    style = typography.title,
                                    color = colors.accent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.cleaner_scan_summary, res.totalScannedFiles, res.totalScannedDirectories),
                                    style = typography.metadata,
                                    color = colors.textSecondary
                                )
                                if (res.truncated) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.cleaner_scan_limited),
                                        style = typography.caption,
                                        color = colors.warning
                                    )
                                }
                            }
                        }
                    }

                    // Junk / Temp files item
                    if (res.tempFiles.isNotEmpty()) {
                        item {
                            CleanerCategoryCard(
                                title = stringResource(R.string.cleaner_cache_temp),
                                subtitle = stringResource(R.string.cleaner_temp_subtitle, res.tempFiles.size, formatSize(res.tempFiles.sumOf { it.length() })),
                                icon = Icons.Outlined.DeleteSweep,
                                checked = cleanTempFiles,
                                onCheckedChange = { cleanTempFiles = it },
                                badgeColor = colors.accent
                            )
                        }
                    }

                    // Obsolete APKs
                    if (res.apkFiles.isNotEmpty()) {
                        item {
                            CleanerCategoryCard(
                                title = stringResource(R.string.cleaner_obsolete_apks),
                                subtitle = stringResource(R.string.cleaner_apk_subtitle, res.apkFiles.size, formatSize(res.apkFiles.sumOf { it.length() })),
                                icon = Icons.Outlined.Android,
                                checked = cleanApks,
                                onCheckedChange = { cleanApks = it },
                                badgeColor = DocumentFormatColors.excelTint
                            )
                        }
                    }

                    // Duplicates
                    if (res.duplicateGroups.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppRadius.cardShape)
                                    .background(colors.surface)
                                    .animateContentSize()
                            ) {
                                CleanerCategoryCard(
                                    title = stringResource(R.string.cleaner_duplicates),
                                    subtitle = stringResource(R.string.cleaner_dup_subtitle, res.duplicateGroups.size, formatSize(res.duplicateGroups.sumOf { it.wastedBytes })),
                                    icon = Icons.Outlined.CopyAll,
                                    checked = cleanDuplicates,
                                    onCheckedChange = { cleanDuplicates = it },
                                    badgeColor = colors.warning,
                                    canExpand = true,
                                    isExpanded = expandDuplicates,
                                    onToggleExpand = { expandDuplicates = !expandDuplicates }
                                )

                                if (expandDuplicates) {
                                    Text("${duplicateKeepers.size} groups reviewed. Choose the copies to keep before cleanup.",
                                        modifier=Modifier.padding(horizontal=16.dp),style=typography.caption)
                                    TextButton(onClick={showDuplicateReview=true},modifier=Modifier.padding(horizontal=8.dp)) {
                                        Text("Review all duplicate groups")
                                    }
                                }
                            }
                        }
                    }

                    // Empty Folders
                    if (res.emptyFolders.isNotEmpty()) {
                        item {
                            CleanerCategoryCard(
                                title = stringResource(R.string.cleaner_empty_dirs),
                                subtitle = stringResource(R.string.cleaner_empty_subtitle, res.emptyFolders.size),
                                icon = Icons.Outlined.FolderOff,
                                checked = cleanEmptyFolders,
                                onCheckedChange = { cleanEmptyFolders = it },
                                badgeColor = colors.textSecondary
                            )
                        }
                    }

                    // Large Files Section
                    if (res.largeFiles.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppRadius.cardShape)
                                    .background(colors.surface)
                                    .animateContentSize()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandLargeFiles = !expandLargeFiles }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SdStorage,
                                        contentDescription = null,
                                        tint = colors.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.cleaner_large_files),
                                            style = typography.bodyStrong,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.cleaner_large_subtitle, res.largeFiles.size, formatSize(res.largeFiles.sumOf { it.length() })),
                                            style = typography.caption,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (expandLargeFiles) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }

                                if (expandLargeFiles) {
                                    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                                        for (file in res.largeFiles.take(8)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onFileClick(file) }
                                                    .padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = file.name,
                                                    style = typography.label,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = formatSize(file.length()),
                                                    style = typography.metadataStrong,
                                                    color = colors.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Nothing to clean state
                    if (res.tempFiles.isEmpty() && res.apkFiles.isEmpty() && res.duplicateGroups.isEmpty() && res.emptyFolders.isEmpty()) {
                        item {
                            AppEmptyState(
                                title = stringResource(R.string.cleaner_clean_title),
                                message = stringResource(R.string.cleaner_clean_message)
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }

                // Bottom Action Button
                if (res.tempFiles.isNotEmpty() || res.apkFiles.isNotEmpty() || res.duplicateGroups.isNotEmpty() || res.emptyFolders.isNotEmpty()) {
                    Surface(
                        color = if (isOled) colors.surfaceContainerHigh else colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp, top = 8.dp)
                    ) {
                        Button(
                            onClick = { showConfirm = true },
                            enabled = !isCleaning && hasSelection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent,
                                disabledContainerColor = colors.surfaceMuted,
                                disabledContentColor = colors.textSecondary
                            ),
                            shape = AppRadius.cardShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            // No explicit colors: the button's own content color is the one
                            // that knows whether this label is live or disabled.
                            if (isCleaning) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(R.string.cleaner_cleaning), style = typography.bodyStrong)
                            } else {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !hasSelection -> stringResource(R.string.cleaner_select_category)
                                        selectedFiles.isEmpty() ->
                                            stringResource(R.string.cleaner_remove_folders, selectedEmptyDirs.size)
                                        else -> stringResource(R.string.cleaner_move_to_trash_size, formatSize(selectedBytesToFree))
                                    },
                                    style = typography.bodyStrong
                                )
                            }
                        }
                    }
                }
            }
        }

    if (showConfirm) {
        AppAlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.cleaner_confirm_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    Text(
                        text = buildString {
                            append("${selectedFiles.size} files (${formatSize(selectedBytesToFree)})")
                            if (selectedEmptyDirs.isNotEmpty()) {
                                append(" and ${selectedEmptyDirs.size} empty folders")
                            }
                            append(" will be processed. You can restore files from Files > Trash for 30 days.")
                        },
                        style = typography.caption,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(AppRadius.smallShape)
                            .background(colors.surfaceContainerHigh)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(candidateFiles, key = { it.absolutePath }) { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = file.absolutePath !in excludedPaths,
                                    onCheckedChange = { checked ->
                                        excludedPaths = if (checked) excludedPaths - file.absolutePath
                                            else excludedPaths + file.absolutePath
                                    },
                                    modifier = Modifier.semantics { contentDescription = file.name }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = typography.micro,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = file.absolutePath,
                                        style = typography.micro,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatSize(file.length()),
                                    style = typography.micro,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        items(candidateEmptyDirs, key = { it.absolutePath }) { dir ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Checkbox(
                                    checked = dir.absolutePath !in excludedPaths,
                                    onCheckedChange = { checked ->
                                        excludedPaths = if (checked) excludedPaths - dir.absolutePath
                                            else excludedPaths + dir.absolutePath
                                    },
                                    modifier = Modifier.semantics { contentDescription = dir.name }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dir.name,
                                        style = typography.micro,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dir.absolutePath,
                                        style = typography.micro,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = hasSelection, onClick = {
                    showConfirm = false
                    coroutineScope.launch {
                        isCleaning = true
                        var partialCleanedCount = 0
                        val moved: List<com.abhisheksati.mydoc.domain.model.TrashItem> =
                            if (selectedFiles.isEmpty()) {
                                emptyList()
                            } else {
                            val duplicatePaths = duplicateCopies(res.duplicateGroups,duplicateKeepers).map { it.absolutePath }.toSet()
                            val verified = verifiedDuplicateCopies(res.duplicateGroups,duplicateKeepers)
                            val safeFiles = selectedFiles.filter { it.absolutePath !in duplicatePaths || it.absolutePath in verified }
                            app.storageRepository.moveToTrash(safeFiles).fold(
                                onSuccess = { it },
                                onFailure = { error ->
                                    if (error is com.abhisheksati.mydoc.domain.model.PartialStorageOperationException) {
                                        partialCleanedCount = error.completed
                                    }
                                    app.showMessage(
                                        UserMessages.actionFailed(R.string.dialog_move_to_bin, error, "Cleaner")
                                            .resolve(app)
                                    )
                                    emptyList()
                                }
                            )
                        }
                        val deletedDirs = cleanerManager.deleteEmptyDirectories(selectedEmptyDirs, rootDir)
                        val actualMovedCount = if (moved.isNotEmpty()) moved.size else partialCleanedCount
                        cleanedCount = actualMovedCount
                        cleanedBytes = if (moved.isNotEmpty()) moved.sumOf { it.sizeBytes } else 0L

                        val failedFiles = selectedFiles.size - actualMovedCount
                        val failedDirs = selectedEmptyDirs.size - deletedDirs
                        if (failedFiles > 0 || failedDirs > 0) {
                            val msg = buildString {
                                append(resources.getString(R.string.cleaner_result_partial, actualMovedCount, deletedDirs))
                                if (failedFiles > 0) {
                                    append(resources.getString(R.string.cleaner_failed_files, failedFiles))
                                }
                                if (failedDirs > 0) {
                                    append(resources.getString(R.string.cleaner_failed_folders, failedDirs))
                                }
                            }
                            app.showMessage(msg)
                        } else {
                            // The files went to the Bin, which sits on the same volume: nothing is
                            // freed until it is emptied, so the message says "reclaimable" rather
                            // than claiming the space back now.
                            app.showMessage(
                                resources.getString(
                                    R.string.cleaner_result_complete,
                                    moved.size,
                                    deletedDirs,
                                    formatSize(cleanedBytes ?: 0L)
                                )
                            )
                        }
                        isCleaning = false
                    }
                }) { Text(stringResource(R.string.dialog_move_to_bin)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
}

@Composable
private fun CleanerCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badgeColor: Color,
    canExpand: Boolean = false,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    GlassSurface(
        shape = AppRadius.cardShape,
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.bodyStrong,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = typography.caption,
                color = colors.textSecondary
            )
        }

        if (canExpand) {
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.cd_expand),
                    tint = colors.textSecondary
                )
            }
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accent,
                uncheckedColor = colors.textSecondary
            )
        )
    }
}
}

private fun formatSize(bytes: Long): String = com.abhisheksati.mydoc.domain.model.StorageVolume.formatBytes(bytes)
