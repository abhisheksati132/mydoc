package com.abhisheksati.mydoc.ui.cleaner

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
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
fun StorageCleanerSheet(
    rootDir: File,
    onDismiss: () -> Unit,
    onFileClick: (File) -> Unit = {}
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val coroutineScope = rememberCoroutineScope()
    val cleanerManager = remember { StorageCleanerManager() }
    val app = LocalContext.current.applicationContext as MyDocApp

    var isScanning by remember { mutableStateOf(true) }
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

    // Expanded states
    var expandDuplicates by remember { mutableStateOf(false) }
    var expandLargeFiles by remember { mutableStateOf(false) }

    // Start scanning on launch
    LaunchedEffect(rootDir) {
        isScanning = true
        val result = cleanerManager.scanDirectory(rootDir) { count, name ->
            scannedCount = count
            currentScannedName = name
        }
        scanResult = result
        isScanning = false
    }

    val isOled = LocalIsOled.current

    val res = scanResult ?: StorageCleanScanResult()
    // Only the extra copies go; the kept original of every duplicate group stays.
    val selectedFiles = buildList {
        if (cleanTempFiles) addAll(res.tempFiles)
        if (cleanApks) addAll(res.apkFiles)
        if (cleanDuplicates) res.duplicateGroups.forEach { addAll(it.duplicates) }
    }.distinctBy { it.absolutePath }
    val selectedEmptyDirs = if (cleanEmptyFolders) res.emptyFolders else emptyList()
    val selectedBytesToFree = selectedFiles.sumOf { it.length() }
    val hasSelection = selectedFiles.isNotEmpty() || selectedEmptyDirs.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        OverlaySystemBars()
        GlassSurface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Storage Cleaner",
                            style = typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Reclaim wasted space & remove junk",
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning) {
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
                        CircularProgressIndicator(
                            color = colors.accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Scanning Storage...",
                            style = typography.bodyStrong,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$scannedCount files analyzed",
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
                            text = "Moved to Trash",
                            style = typography.heading,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$cleanedCount items (${formatSize(cleanedBytes ?: 0L)}) moved to Trash. " +
                                "Empty Trash to actually free the space.",
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
                            Text("Done", color = Color.White)
                        }
                    }
                }
            } else {
                // Results list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary card
                    item {
                        Surface(
                            color = colors.accent.copy(alpha = 0.1f),
                            shape = AppRadius.largeShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Reclaimable Storage",
                                    style = typography.caption,
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
                                    text = "${res.totalScannedFiles} files scanned across ${res.totalScannedDirectories} folders",
                                    style = typography.micro,
                                    color = colors.textSecondary
                                )
                                if (res.truncated) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Scan stopped at the safety limit — some files were not checked.",
                                        style = typography.micro,
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
                                title = "Cache & Temporary Files",
                                subtitle = "${res.tempFiles.size} junk files (${formatSize(res.tempFiles.sumOf { it.length() })})",
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
                                title = "Obsolete APK Packages",
                                subtitle = "${res.apkFiles.size} installer files (${formatSize(res.apkFiles.sumOf { it.length() })})",
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
                                    title = "Duplicate Files",
                                    subtitle = "${res.duplicateGroups.size} identical file clusters (${formatSize(res.duplicateGroups.sumOf { it.wastedBytes })})",
                                    icon = Icons.Outlined.CopyAll,
                                    checked = cleanDuplicates,
                                    onCheckedChange = { cleanDuplicates = it },
                                    badgeColor = colors.warning,
                                    canExpand = true,
                                    isExpanded = expandDuplicates,
                                    onToggleExpand = { expandDuplicates = !expandDuplicates }
                                )

                                if (expandDuplicates) {
                                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                                        Text(
                                            text = "Keeping 1 original copy, removing duplicates:",
                                            style = typography.micro,
                                            color = colors.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        for (group in res.duplicateGroups.take(10)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = group.original.name,
                                                        style = typography.label,
                                                        color = colors.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${group.duplicates.size} duplicate(s)",
                                                        style = typography.micro,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                Text(
                                                    text = formatSize(group.wastedBytes),
                                                    style = typography.captionStrong,
                                                    color = colors.warning
                                                )
                                            }
                                        }
                                        if (res.duplicateGroups.size > 10) {
                                            Text(
                                                text = "+ ${res.duplicateGroups.size - 10} more duplicate groups",
                                                style = typography.micro,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Empty Folders
                    if (res.emptyFolders.isNotEmpty()) {
                        item {
                            CleanerCategoryCard(
                                title = "Empty Directories",
                                subtitle = "${res.emptyFolders.size} zero-item folders taking clutter",
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
                                            text = "Large Files (>50 MB)",
                                            style = typography.bodyStrong,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "${res.largeFiles.size} large files (${formatSize(res.largeFiles.sumOf { it.length() })})",
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
                                                    style = typography.captionStrong,
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ThumbUp,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Storage is Clean!",
                                        style = typography.bodyStrong,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "No unnecessary junk or duplicate files found.",
                                        style = typography.label,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }

                // Bottom Action Button
                if (res.tempFiles.isNotEmpty() || res.apkFiles.isNotEmpty() || res.duplicateGroups.isNotEmpty() || res.emptyFolders.isNotEmpty()) {
                    Surface(
                        color = if (isOled) colors.surfaceContainerHigh else colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp, top = 8.dp)
                    ) {
                        Button(
                            onClick = { showConfirm = true },
                            enabled = !isCleaning && hasSelection,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = AppRadius.cardShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isCleaning) {
                                CircularProgressIndicator(
                                    color = colors.onAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Cleaning...", style = typography.bodyStrong, color = colors.onAccent)
                            } else {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.onAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !hasSelection -> "Select a category to clean"
                                        selectedFiles.isEmpty() ->
                                            "Remove ${selectedEmptyDirs.size} empty folders"
                                        else -> "Move to Trash (${formatSize(selectedBytesToFree)})"
                                    },
                                    style = typography.bodyStrong,
                                    color = colors.onAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Move to Trash?") },
            text = {
                Text(
                    buildString {
                        append("${selectedFiles.size} files (${formatSize(selectedBytesToFree)})")
                        if (selectedEmptyDirs.isNotEmpty()) {
                            append(" and ${selectedEmptyDirs.size} empty folders")
                        }
                        append(" will be moved to Trash. You can restore files from Files > Trash for 30 days.")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    coroutineScope.launch {
                        isCleaning = true
                        val moved: List<com.abhisheksati.mydoc.domain.model.TrashItem> =
                            if (selectedFiles.isEmpty()) {
                                emptyList()
                            } else {
                            app.storageRepository.moveToTrash(selectedFiles).getOrElse { error ->
                                app.showMessage("Could not move files: ${error.message}")
                                emptyList()
                            }
                        }
                        cleanerManager.deleteEmptyDirectories(selectedEmptyDirs)
                        cleanedCount = moved.size
                        cleanedBytes = moved.sumOf { it.sizeBytes }
                        isCleaning = false
                    }
                }) { Text("Move to Trash") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
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
                    contentDescription = "Expand",
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
