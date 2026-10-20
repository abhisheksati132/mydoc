package com.abhisheksati.mydoc.ui.storage

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.ui.appmanager.AppManagerSheet
import com.abhisheksati.mydoc.ui.cleaner.StorageCleanerSheet
import java.io.File

@Composable
fun StorageScreen(
    viewModel: StorageViewModel,
    onNavigateToCategory: (FileCategory) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = colors.background == DarkAppColors.background

    var showCleanerSheet by remember { mutableStateOf(false) }
    var showAppManagerSheet by remember { mutableStateOf(false) }
    var showSystemInfoDialog by remember { mutableStateOf(false) }
    var isVolumeDropdownOpen by remember { mutableStateOf(false) }

    // Rotation animation for refresh icon
    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            rotationAnim.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotationAnim.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Storage Analyzer",
                        style = typography.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )

                    // Volume selection chip
                    if (uiState.volumes.size > 1) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isVolumeDropdownOpen = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.activeVolume?.title ?: "Internal Storage",
                                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.accent
                                )
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            val isOled = colors.background == DarkAppColors.background
                            DropdownMenu(
                                expanded = isVolumeDropdownOpen,
                                onDismissRequest = { isVolumeDropdownOpen = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = if (isOled) Color(0xFF141720) else colors.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isOled) Color.White.copy(alpha = 0.10f) else colors.divider.copy(alpha = 0.7f)
                                ),
                                shadowElevation = 8.dp,
                                tonalElevation = 4.dp
                            ) {
                                uiState.volumes.forEachIndexed { idx, vol ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${vol.title} (${vol.formattedTotal})",
                                                fontWeight = if (idx == uiState.selectedVolumeIndex) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectVolume(idx)
                                            isVolumeDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = uiState.activeVolume?.title ?: "Device Storage",
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refresh() }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh Storage",
                            tint = colors.textPrimary,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(rotationAnim.value)
                        )
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (uiState.isLoading && uiState.categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.accent, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Analyzing storage...",
                            style = typography.body,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // 1. Hero Storage Master Card
                    item {
                        StorageHeroCard(
                            uiState = uiState,
                            isOled = isOled
                        )
                    }

                    // 2. Quick Clean & Health Recommendations
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        StorageQuickCleanSection(
                            uiState = uiState,
                            isOled = isOled,
                            onCleanClick = { showCleanerSheet = true },
                            onOpenLargeFiles = { onNavigateToCategory(FileCategory.LARGE_FILES) },
                            onManageAppsClick = { showAppManagerSheet = true }
                        )
                    }

                    // 3. Category Breakdown Header
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Storage Breakdown",
                                style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${uiState.categories.size} categories",
                                style = typography.caption,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 4. Category breakdown items
                    items(
                        items = uiState.categories,
                        key = { it.type.name }
                    ) { stat ->
                        StorageCategoryRowItem(
                            stat = stat,
                            isOled = isOled,
                            onClick = {
                                if (stat.type.fileCategory != null) {
                                    onNavigateToCategory(stat.type.fileCategory)
                                } else if (stat.type == StorageCategoryType.APPS) {
                                    showAppManagerSheet = true
                                } else if (stat.type == StorageCategoryType.SYSTEM_OTHER) {
                                    showSystemInfoDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Storage Cleaner bottom sheet
    if (showCleanerSheet) {
        val rootDir = uiState.activeVolume?.file ?: File("/storage/emulated/0")
        StorageCleanerSheet(
            rootDir = rootDir,
            onDismiss = {
                showCleanerSheet = false
                viewModel.refresh()
            }
        )
    }

    // App Manager bottom sheet
    if (showAppManagerSheet) {
        AppManagerSheet(
            onDismiss = {
                showAppManagerSheet = false
                viewModel.refresh()
            }
        )
    }

    // System & Other explanation dialog
    if (showSystemInfoDialog) {
        val isOled = colors.background == DarkAppColors.background
        AlertDialog(
            onDismissRequest = { showSystemInfoDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = if (isOled) Color(0xFF0E0F14) else colors.surface,
            modifier = Modifier.border(
                BorderStroke(
                    0.8.dp,
                    if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(22.dp)
            ),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.accent
                )
            },
            title = {
                Text(
                    text = "System & Other Storage",
                    fontWeight = FontWeight.Bold,
                    style = typography.title
                )
            },
            text = {
                Text(
                    text = "This space contains the Android operating system binaries, system runtime partitions, internal caches, and app-private sandboxed database files that cannot be directly accessed without root privileges.\n\nTo reclaim app data space, you can inspect individual apps in App Manager.",
                    style = typography.body,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSystemInfoDialog = false
                        showAppManagerSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Manage Apps")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun StorageHeroCard(
    uiState: StorageAnalysisUiState,
    isOled: Boolean
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val healthBadgeText = when {
        uiState.freeBytes > 15L * 1024 * 1024 * 1024 -> "Healthy Storage"
        uiState.freeBytes > 4L * 1024 * 1024 * 1024 -> "Moderate Storage"
        else -> "Storage Running Low"
    }

    val healthBadgeColor = when {
        uiState.freeBytes > 15L * 1024 * 1024 * 1024 -> Color(0xFF4CAF50)
        uiState.freeBytes > 4L * 1024 * 1024 * 1024 -> Color(0xFFFFA000)
        else -> Color(0xFFE53935)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isOled) Color(0xFF101217) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Top row with Used % and Health Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${uiState.usedPercentage}%",
                        style = typography.title.copy(fontSize = 38.sp, fontWeight = FontWeight.ExtraBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "used",
                        style = typography.body.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = healthBadgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, healthBadgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = healthBadgeText,
                        style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = healthBadgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle size
            Text(
                text = "${uiState.formattedUsed} used of ${uiState.formattedTotal} • ${uiState.formattedFree} free",
                style = typography.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Segmented Storage Visualizer Bar
            SegmentedStorageBar(
                categories = uiState.categories,
                totalBytes = uiState.totalBytes,
                usedBytes = uiState.usedBytes,
                freeBytes = uiState.freeBytes,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Color legend row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.categories.filter { it.bytes > 0L }.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(cat.type.color)
                        )
                        Text(
                            text = "${cat.type.title}: ${cat.formattedSize}",
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedStorageBar(
    categories: List<StorageCategoryStat>,
    totalBytes: Long,
    usedBytes: Long,
    freeBytes: Long,
    modifier: Modifier = Modifier
) {
    val barHeight = 14.dp
    val freeColor = Color(0x33888888)

    Box(
        modifier = modifier
            .height(barHeight)
            .clip(RoundedCornerShape(7.dp))
            .background(freeColor)
    ) {
        if (totalBytes > 0L) {
            Row(modifier = Modifier.fillMaxSize()) {
                val nonZero = categories.filter { it.bytes > 0L }
                for (cat in nonZero) {
                    val weight = (cat.bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.005f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(barHeight)
                            .background(cat.type.color)
                    )
                }

                val freeWeight = (freeBytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.001f)
                if (freeWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(freeWeight)
                            .height(barHeight)
                            .background(Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageQuickCleanSection(
    uiState: StorageAnalysisUiState,
    isOled: Boolean,
    onCleanClick: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onManageAppsClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val clean = uiState.cleanSummary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Quick clean master banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isOled) Color(0xFF141720) else colors.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (clean.totalReclaimableBytes > 0L) {
                                "${StorageVolume.formatBytes(clean.totalReclaimableBytes)} Reclaimable"
                            } else "Storage Optimizer",
                            style = typography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (uiState.isScanningCleaner) "Scanning for junk & duplicates..."
                            else "${clean.tempFilesCount} temp files • ${clean.duplicateGroupsCount} duplicate sets",
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.textSecondary
                        )
                    }
                }

                Button(
                    onClick = onCleanClick,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = if (isOled) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Clean Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Diagnostic action chips row (Large files, Duplicates, Apps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Large Files card
            StorageMiniCard(
                title = "Large Files",
                countText = "${clean.largeFilesCount} files",
                sizeText = StorageVolume.formatBytes(clean.largeFilesBytes),
                icon = Icons.Outlined.DataUsage,
                accentColor = Color(0xFFFF5722),
                isOled = isOled,
                onClick = onOpenLargeFiles,
                modifier = Modifier.weight(1f)
            )

            // Duplicate Files card
            StorageMiniCard(
                title = "Duplicates",
                countText = "${clean.duplicateGroupsCount} sets",
                sizeText = StorageVolume.formatBytes(clean.duplicateWastedBytes),
                icon = Icons.Outlined.DeleteSweep,
                accentColor = Color(0xFF009688),
                isOled = isOled,
                onClick = onCleanClick,
                modifier = Modifier.weight(1f)
            )

            // Apps space card
            StorageMiniCard(
                title = "User Apps",
                countText = "${uiState.userAppCount} apps",
                sizeText = StorageVolume.formatBytes(uiState.userAppBytes),
                icon = Icons.Outlined.Apps,
                accentColor = Color(0xFF2196F3),
                isOled = isOled,
                onClick = onManageAppsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StorageMiniCard(
    title: String,
    countText: String,
    sizeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isOled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOled) Color(0xFF101217) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.06f) else colors.divider.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sizeText,
                style = typography.title.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = countText,
                style = typography.caption.copy(fontSize = 10.sp),
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StorageCategoryRowItem(
    stat: StorageCategoryStat,
    isOled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isOled) Color(0xFF101217) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isOled) Color.White.copy(alpha = 0.05f) else colors.divider.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(stat.type.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = stat.type.icon,
                        contentDescription = null,
                        tint = stat.type.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.type.title,
                        style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (stat.type == StorageCategoryType.APPS) "${stat.fileCount} installed apps"
                        else if (stat.type == StorageCategoryType.SYSTEM_OTHER) "System & App data partitions"
                        else "${stat.fileCount} files",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { stat.percentageOfUsed },
                        color = stat.type.color,
                        trackColor = if (isOled) Color(0xFF1E2026) else colors.divider,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stat.formattedSize,
                        style = typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${(stat.percentageOfUsed * 100f).toInt()}%",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
