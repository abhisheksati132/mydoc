package com.abhisheksati.mydoc.tools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel

import androidx.compose.foundation.layout.Arrangement

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onToolClick: (PdfTool) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    onOpenTrash: (() -> Unit)? = null,
    trashCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp
    var showWifiSharingSheet by remember { mutableStateOf(false) }
    var showStorageCleanerSheet by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    val allTools = remember { PdfTool.entries }
    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allTools
        } else {
            val q = searchQuery.trim().lowercase()
            allTools.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.group.title.lowercase().contains(q)
            }
        }
    }

    val groupedTools = remember(filteredTools) {
        filteredTools.groupBy { it.group }
    }

    var isSearchActive by remember { mutableStateOf(searchQuery.isNotEmpty()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AppTabHeader(
            title = "Tools",
            onSearchClick = { isSearchActive = !isSearchActive },
            isSearchActive = isSearchActive || searchQuery.isNotEmpty(),
            searchContent = {
                AppSearchField(
                    query = searchQuery,
                    onQueryChanged = { searchQuery = it },
                    placeholder = "Search 30 PDF tools...",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onSettingsClick = onSettingsClick,
            moreMenuContent = { onDismiss ->
                if (onOpenTrash != null) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Recycle Bin", style = typography.body)
                                if (trashCount > 0) {
                                    Badge { Text(trashCount.toString()) }
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.accent)
                        },
                        onClick = {
                            onDismiss()
                            onOpenTrash()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Scan Document to PDF", style = typography.body) },
                    leadingIcon = {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = colors.accent)
                    },
                    onClick = {
                        onDismiss()
                        onToolClick(PdfTool.SCAN_TO_PDF)
                    }
                )
                if (onSettingsClick != null) {
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
                }
            }
        )

        if (filteredTools.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.paddingLarge),
                contentAlignment = Alignment.Center
            ) {
                AppEmptyState(
                    title = "No tools found",
                    message = "No tools matched \"$searchQuery\"."
                )
            }
        } else {
            // Plain list grouped by section headers
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 88.dp)
            ) {
                if (searchQuery.isBlank()) {
                    item(key = "super_tools_header") {
                        AppSectionHeader(
                            title = "SUPER POWER TOOLS",
                            modifier = Modifier.padding(top = AppSpacing.paddingSmall)
                        )
                    }

                    item(key = "super_tools_card") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showWifiSharingSheet = true
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1B2A4A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Wifi,
                                            contentDescription = null,
                                            tint = Color(0xFF63B3ED),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "PC Wi-Fi Transfer",
                                                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge(containerColor = colors.accent, contentColor = Color.Black) {
                                                Text("FAST", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            text = "Transfer files to/from PC without cables at Wi-Fi speeds",
                                            style = typography.caption,
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = colors.divider,
                                    modifier = Modifier.padding(start = 70.dp, end = 14.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showStorageCleanerSheet = true
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF261010)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteSweep,
                                            contentDescription = null,
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Storage Cleaner & Duplicates",
                                                style = typography.body.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge(containerColor = Color(0xFFEF5350), contentColor = Color.White) {
                                                Text("CLEAN", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            text = "Find duplicate files, large files & remove cache junk",
                                            style = typography.caption,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                ToolGroup.entries.forEach { group ->
                    val toolsInGroup = groupedTools[group]
                    if (!toolsInGroup.isNullOrEmpty()) {
                        item(key = "header_${group.name}") {
                            AppSectionHeader(
                                title = group.title,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        item(key = "card_${group.name}") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column {
                                    toolsInGroup.forEachIndexed { index, tool ->
                                        ToolListRow(
                                            tool = tool,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onToolClick(tool)
                                            }
                                        )
                                        if (index < toolsInGroup.size - 1) {
                                            HorizontalDivider(
                                                color = colors.divider,
                                                modifier = Modifier.padding(start = 68.dp, end = 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        if (showWifiSharingSheet && app != null) {
            com.abhisheksati.mydoc.ui.network.WifiSharingSheet(
                wifiServerManager = app.wifiServerManager,
                rootDirectory = android.os.Environment.getExternalStorageDirectory(),
                onDismiss = { showWifiSharingSheet = false }
            )
        }

        if (showStorageCleanerSheet) {
            com.abhisheksati.mydoc.ui.cleaner.StorageCleanerSheet(
                rootDir = android.os.Environment.getExternalStorageDirectory(),
                onDismiss = { showStorageCleanerSheet = false }
            )
        }
    }
}

@Composable
private fun ToolListRow(
    tool: PdfTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isDark = colors.background == DarkAppColors.background

    val (groupTint, groupContainer) = when (tool.group) {
        ToolGroup.ORGANIZE -> Color(0xFF1A73E8) to if (isDark) Color(0xFF1B2A4A) else Color(0xFFE8F0FE)
        ToolGroup.OPTIMIZE -> Color(0xFF188038) to if (isDark) Color(0xFF1B3824) else Color(0xFFE6F4EA)
        ToolGroup.CONVERT_TO -> Color(0xFF9334E6) to if (isDark) Color(0xFF2D1E4A) else Color(0xFFF3E8FD)
        ToolGroup.CONVERT_FROM -> Color(0xFF3F51B5) to if (isDark) Color(0xFF1E2448) else Color(0xFFE8EAF6)
        ToolGroup.EDIT -> Color(0xFFF29900) to if (isDark) Color(0xFF3A301A) else Color(0xFFFEF7E0)
        ToolGroup.SECURITY -> Color(0xFFEA4335) to if (isDark) Color(0xFF381E1E) else Color(0xFFFCE8E6)
        ToolGroup.SCAN -> Color(0xFF00897B) to if (isDark) Color(0xFF1A3834) else Color(0xFFE0F2F1)
        ToolGroup.AUTOMATION -> Color(0xFF0284C7) to if (isDark) Color(0xFF162E4A) else Color(0xFFE0F2FE)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Soft-tinted squircle icon (Google Files style)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(groupContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = groupTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and calm single-sentence description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                style = typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool.description,
                style = typography.caption,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}
