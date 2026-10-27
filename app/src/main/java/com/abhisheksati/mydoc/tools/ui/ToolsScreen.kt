package com.abhisheksati.mydoc.tools.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.BricolageFontFamily
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel

private data class ToolNavCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    val color: Color,
    val label: String
)

/**
 * Material 3 Expressive Tools Dashboard.
 *
 * User-friendly, plain English, zero developer jargon:
 * 1. Prominent "Popular Essentials" 2-column cards on top (Scan, Merge, Compress, Sign, Organize, Protect).
 * 2. Intuitive Category Filter Chips.
 * 3. Clear, friendly list of all tools with plain human benefits.
 */
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onToolClick: (PdfTool) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

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
        // App Header
        AppTabHeader(
            title = "Tools",
            titleContent = {
                Text(
                    text = "Tools",
                    fontFamily = BricolageFontFamily,
                    style = typography.headline.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp
                    ),
                    color = colors.ink
                )
            },
            onSearchClick = { isSearchActive = !isSearchActive },
            isSearchActive = isSearchActive || searchQuery.isNotEmpty(),
            searchContent = {
                AppSearchField(
                    query = searchQuery,
                    onQueryChanged = { searchQuery = it },
                    placeholder = "Search tools...",
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
                    ToolNavCategory(ToolGroup.OPTIMIZE, Icons.Outlined.Compress, Color(0xFF10B981), "Compress"),
                    ToolNavCategory(ToolGroup.CONVERT_TO, Icons.Outlined.Transform, Color(0xFF8B5CF6), "Convert"),
                    ToolNavCategory(ToolGroup.EDIT, Icons.Outlined.Edit, Color(0xFFF59E0B), "Sign & Edit"),
                    ToolNavCategory(ToolGroup.SECURITY, Icons.Outlined.Security, Color(0xFFEF4444), "Security"),
                    ToolNavCategory(ToolGroup.SCAN, Icons.Outlined.CameraAlt, Color(0xFF14B8A6), "Scan")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (item in toolCategories) {
                        val isSelected = selectedToolGroup == item.group
                        val chipBg = if (isSelected) item.color.copy(alpha = 0.16f) else colors.surfaceContainer
                        val chipBorderColor = if (isSelected) item.color else colors.cardBorder

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = chipBg,
                            border = BorderStroke(1.dp, chipBorderColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedToolGroup = if (isSelected) null else item.group
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) item.color else colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    style = typography.caption.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) item.color else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            },
            moreMenuContent = { onDismiss ->
                DropdownMenuItem(
                    text = { Text("Scan Document", style = typography.body) },
                    leadingIcon = {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color(0xFF14B8A6))
                    },
                    onClick = {
                        onDismiss()
                        onToolClick(PdfTool.SCAN_TO_PDF)
                    }
                )
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp)
            ) {
                // When not searching and no filter active: show Featured Everyday Essentials
                if (searchQuery.isBlank() && selectedToolGroup == null) {
                    item(key = "featured_tools_header") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            Text(
                                text = "Popular Essentials",
                                fontFamily = BricolageFontFamily,
                                style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Frequently used actions",
                                style = typography.caption.copy(fontSize = 12.sp),
                                color = colors.inkMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 2-Column Grid of 6 Featured Tools
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FeaturedToolCard(
                                    title = "Scan to PDF",
                                    subtitle = "Camera to document",
                                    icon = Icons.Outlined.CameraAlt,
                                    color = Color(0xFF14B8A6),
                                    isDark = isDarkTheme,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToolClick(PdfTool.SCAN_TO_PDF)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedToolCard(
                                    title = "Combine PDFs",
                                    subtitle = "Merge files together",
                                    icon = Icons.AutoMirrored.Outlined.MergeType,
                                    color = Color(0xFF3B82F6),
                                    isDark = isDarkTheme,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToolClick(PdfTool.MERGE)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FeaturedToolCard(
                                    title = "Make Smaller",
                                    subtitle = "Reduce PDF file size",
                                    icon = Icons.Outlined.Compress,
                                    color = Color(0xFF10B981),
                                    isDark = isDarkTheme,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToolClick(PdfTool.COMPRESS)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedToolCard(
                                    title = "Sign PDF",
                                    subtitle = "Add your signature",
                                    icon = Icons.Outlined.Draw,
                                    color = Color(0xFFF59E0B),
                                    isDark = isDarkTheme,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToolClick(PdfTool.SIGN_PDF)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FeaturedToolCard(
                                    title = "Pick Pages",
                                    subtitle = "Reorder & rotate",
                                    icon = Icons.Outlined.GridOn,
                                    color = Color(0xFF8B5CF6),
                                    isDark = isDarkTheme,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToolClick(PdfTool.REORDER_PAGES)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedToolCard(
                                    title = "Lock PDF",
                                    subtitle = "Protect with password",
                                    icon = Icons.Outlined.Lock,
                                    color = Color(0xFFEF4444),
                                    isDark = isDarkTheme,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToolClick(PdfTool.PROTECT)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Grouped Tools List
                val groupsToDisplay = if (selectedToolGroup != null) {
                    listOf(selectedToolGroup!!)
                } else {
                    ToolGroup.entries
                }

                groupsToDisplay.forEach { group ->
                    val toolsInGroup = groupedTools[group]
                    if (!toolsInGroup.isNullOrEmpty()) {
                        val friendlyHeader = when (group) {
                            ToolGroup.ORGANIZE -> "Organize & Pages"
                            ToolGroup.OPTIMIZE -> "Compress & Optimize"
                            ToolGroup.CONVERT_TO -> "Convert to PDF"
                            ToolGroup.CONVERT_FROM -> "Convert from PDF"
                            ToolGroup.EDIT -> "Sign & Annotate"
                            ToolGroup.SECURITY -> "Privacy & Protection"
                            ToolGroup.SCAN -> "Camera & OCR"
                        }

                        item(key = "header_${group.name}") {
                            AppSectionHeader(
                                title = friendlyHeader,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item(key = "card_${group.name}") {
                            val isOled = colors.background == DarkAppColors.background
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isOled) colors.surface else colors.surfaceContainer,
                                border = BorderStroke(1.dp, colors.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
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
                                                modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 22dp Rounded Material 3 Expressive Featured Tool Card.
 */
@Composable
private fun FeaturedToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalAppTypography.current
    val containerColor = if (isDark) color.copy(alpha = 0.14f) else color.copy(alpha = 0.08f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(1.dp, color.copy(alpha = if (isDark) 0.3f else 0.18f)),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = color.copy(alpha = if (isDark) 0.35f else 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontFamily = BricolageFontFamily,
                style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 11.5.sp),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
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
    }

    // Friendly, user-oriented badges (no developer jargon)
    val friendlyBadge: String? = when (tool) {
        PdfTool.SCAN_TO_PDF -> "Camera"
        PdfTool.COMPRESS -> "Popular"
        PdfTool.PROTECT -> "Lock"
        PdfTool.UNLOCK -> "Unlock"
        PdfTool.MERGE -> "Popular"
        PdfTool.SIGN_PDF -> "Signature"
        PdfTool.REORDER_PAGES -> "Visual"
        PdfTool.IMAGE_TO_PDF -> "Easy"
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expressive Squircle container
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
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

        if (friendlyBadge != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = groupTint.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, groupTint.copy(alpha = 0.25f)),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = friendlyBadge,
                    style = typography.caption.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = groupTint,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                )
            }
        }
    }
}
