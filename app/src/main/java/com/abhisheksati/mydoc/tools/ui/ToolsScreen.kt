package com.abhisheksati.mydoc.tools.ui

import androidx.compose.foundation.BorderStroke
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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement

private data class ToolNavCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    val color: Color,
    val label: String
)

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
    var selectedToolGroup by remember { mutableStateOf<ToolGroup?>(null) }

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
            bottomContent = {
                val toolCategories = listOf(
                    ToolNavCategory(null, Icons.Outlined.AllInclusive, colors.accent, "All"),
                    ToolNavCategory(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, Color(0xFF3B82F6), "Organize"),
                    ToolNavCategory(ToolGroup.OPTIMIZE, Icons.Outlined.Compress, Color(0xFF10B981), "Optimize"),
                    ToolNavCategory(ToolGroup.CONVERT_TO, Icons.Outlined.Transform, Color(0xFF8B5CF6), "Convert To"),
                    ToolNavCategory(ToolGroup.CONVERT_FROM, Icons.Outlined.Difference, Color(0xFF6366F1), "Convert From"),
                    ToolNavCategory(ToolGroup.EDIT, Icons.Outlined.Edit, Color(0xFFF59E0B), "Edit"),
                    ToolNavCategory(ToolGroup.SECURITY, Icons.Outlined.Security, Color(0xFFEF4444), "Security"),
                    ToolNavCategory(ToolGroup.SCAN, Icons.Outlined.CameraAlt, Color(0xFF14B8A6), "Scan & OCR"),
                    ToolNavCategory(ToolGroup.AUTOMATION, Icons.Outlined.AutoMode, Color(0xFF06B6D4), "Automation")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (item in toolCategories) {
                        val isSelected = selectedToolGroup == item.group
                        val chipBg = if (isSelected) item.color.copy(alpha = 0.16f) else colors.surfaceVariant
                        val chipBorderColor = if (isSelected) item.color else colors.cardBorder

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = chipBg,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, chipBorderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedToolGroup = item.group
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) item.color else item.color.copy(alpha = 0.85f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    style = typography.caption.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            },
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
                DropdownMenuItem(
                    text = { Text("PC Wi-Fi Transfer", style = typography.body) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Wifi, contentDescription = null, tint = colors.accent)
                    },
                    onClick = {
                        onDismiss()
                        showWifiSharingSheet = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Storage Cleaner", style = typography.body) },
                    leadingIcon = {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.accent)
                    },
                    onClick = {
                        onDismiss()
                        showStorageCleanerSheet = true
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 104.dp)
            ) {
                val groupsToDisplay = if (selectedToolGroup != null) {
                    listOf(selectedToolGroup!!)
                } else {
                    ToolGroup.entries
                }

                groupsToDisplay.forEach { group ->
                    val toolsInGroup = groupedTools[group]
                    if (!toolsInGroup.isNullOrEmpty()) {
                        item(key = "header_${group.name}") {
                            AppSectionHeader(
                                title = group.title,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        item(key = "card_${group.name}") {
                            val isOled = colors.background == DarkAppColors.background
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isOled) colors.surface else colors.surface,
                                border = BorderStroke(1.dp, colors.cardBorder),
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
                                                color = colors.divider.copy(alpha = 0.5f),
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
        ToolGroup.ORGANIZE -> Color(0xFF3B82F6) to if (isDark) Color(0x283B82F6) else Color(0xFFDBEAFE)
        ToolGroup.OPTIMIZE -> Color(0xFF10B981) to if (isDark) Color(0x2810B981) else Color(0xFFD1FAE5)
        ToolGroup.CONVERT_TO -> Color(0xFF8B5CF6) to if (isDark) Color(0x288B5CF6) else Color(0xFFEDE9FE)
        ToolGroup.CONVERT_FROM -> Color(0xFF6366F1) to if (isDark) Color(0x286366F1) else Color(0xFFE0E7FF)
        ToolGroup.EDIT -> Color(0xFFF59E0B) to if (isDark) Color(0x28F59E0B) else Color(0xFFFEF3C7)
        ToolGroup.SECURITY -> Color(0xFFEF4444) to if (isDark) Color(0x28EF4444) else Color(0xFFFEE2E2)
        ToolGroup.SCAN -> Color(0xFF14B8A6) to if (isDark) Color(0x2814B8A6) else Color(0xFFCCFBF1)
        ToolGroup.AUTOMATION -> Color(0xFF06B6D4) to if (isDark) Color(0x2806B6D4) else Color(0xFFCFFAFE)
    }

    val specialtyBadge: String? = when (tool) {
        PdfTool.SCAN_TO_PDF -> "OCR"
        PdfTool.COMPRESS -> "Lossless"
        PdfTool.PROTECT -> "AES-256"
        PdfTool.UNLOCK -> "Key"
        PdfTool.PDF_TO_PDFA -> "Archival"
        PdfTool.MERGE -> "Batch"
        PdfTool.SIGN_PDF -> "Secure"
        PdfTool.SPLIT -> "Extract"
        PdfTool.ADD_WATERMARK -> "Stamp"
        PdfTool.TEXT_TO_PDF -> "Doc"
        PdfTool.PDF_TO_IMAGES -> "Render"
        PdfTool.PDF_TO_TEXT_MD -> "Markdown"
        PdfTool.OCR_PDF -> "Searchable"
        PdfTool.WORKFLOW -> "Chain"
        PdfTool.REPAIR -> "Fix"
        PdfTool.REDACT -> "Sanitize"
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Neo-Precision Squircle icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(groupContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = groupTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = colors.textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tool.description,
                style = typography.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                maxLines = 1
            )
        }

        if (specialtyBadge != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = groupTint.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, groupTint.copy(alpha = 0.25f)),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = specialtyBadge,
                    style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = groupTint,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
