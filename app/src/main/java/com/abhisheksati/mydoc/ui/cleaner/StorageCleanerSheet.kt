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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
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

    var isScanning by remember { mutableStateOf(true) }
    var scannedCount by remember { mutableIntStateOf(0) }
    var currentScannedName by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<StorageCleanScanResult?>(null) }
    var isCleaning by remember { mutableStateOf(false) }
    var cleanedBytes by remember { mutableStateOf<Long?>(null) }

    // Category selection states
    var cleanTempFiles by remember { mutableStateOf(true) }
    var cleanApks by remember { mutableStateOf(true) }
    var cleanDuplicates by remember { mutableStateOf(true) }
    var cleanEmptyFolders by remember { mutableStateOf(true) }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        OverlaySystemBars()
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
                            text = "Clean Up Complete!",
                            style = typography.heading,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Successfully freed ${formatSize(cleanedBytes ?: 0L)} of storage space",
                            style = typography.body,
                            color = colors.textSecondary
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
                val res = scanResult ?: StorageCleanScanResult()

                // Calculate currently selected bytes to free
                val selectedBytesToFree = (if (cleanTempFiles) res.tempFiles.sumOf { it.length() } else 0L) +
                        (if (cleanApks) res.apkFiles.sumOf { it.length() } else 0L) +
                        (if (cleanDuplicates) res.duplicateGroups.sumOf { it.wastedBytes } else 0L)

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
                if (selectedBytesToFree > 0 || cleanEmptyFolders && res.emptyFolders.isNotEmpty()) {
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
                            onClick = {
                                coroutineScope.launch {
                                    isCleaning = true
                                    val filesToDelete = mutableListOf<File>()
                                    if (cleanTempFiles) filesToDelete.addAll(res.tempFiles)
                                    if (cleanApks) filesToDelete.addAll(res.apkFiles)
                                    if (cleanDuplicates) {
                                        for (group in res.duplicateGroups) {
                                            filesToDelete.addAll(group.duplicates)
                                        }
                                    }
                                    if (cleanEmptyFolders) filesToDelete.addAll(res.emptyFolders)

                                    val freed = cleanerManager.cleanFiles(filesToDelete)
                                    cleanedBytes = freed
                                    isCleaning = false
                                }
                            },
                            enabled = !isCleaning,
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
                                    text = "Clean Selected (${formatSize(selectedBytesToFree)})",
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.cardShape)
            .background(colors.surface)
            .clickable { onCheckedChange(!checked) }
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

private fun formatSize(bytes: Long): String = com.abhisheksati.mydoc.domain.model.StorageVolume.formatBytes(bytes)
