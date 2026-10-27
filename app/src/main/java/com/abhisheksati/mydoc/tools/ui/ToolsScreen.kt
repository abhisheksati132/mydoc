package com.abhisheksati.mydoc.tools.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class ToolNavCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    val label: String
)

/**
 * Material 3 Expressive Tools Dashboard.
 *
 * User-friendly, plain English, unified cohesive palette:
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
                    ToolNavCategory(null, Icons.Outlined.AllInclusive, "All"),
                    ToolNavCategory(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, "Organize"),
                    ToolNavCategory(ToolGroup.OPTIMIZE, Icons.Outlined.Compress, "Compress"),
                    ToolNavCategory(ToolGroup.CONVERT_TO, Icons.Outlined.Transform, "Convert"),
                    ToolNavCategory(ToolGroup.EDIT, Icons.Outlined.Edit, "Sign & Edit"),
                    ToolNavCategory(ToolGroup.SECURITY, Icons.Outlined.Security, "Security"),
                    ToolNavCategory(ToolGroup.SCAN, Icons.Outlined.CameraAlt, "Scan")
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
                        val chipBg = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surfaceContainer
                        val contentColor = if (isSelected) colors.accent else colors.textSecondary

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = chipBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
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
                                    tint = contentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    style = typography.caption.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) colors.accent else colors.textPrimary
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
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = colors.accent)
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
                                style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Frequently used actions",
                                style = typography.caption.copy(fontSize = 12.sp),
                                color = colors.textSecondary
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
                                    onClick = { onToolClick(PdfTool.SCAN_TO_PDF) },
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedToolCard(
                                    title = "Combine PDFs",
                                    subtitle = "Merge files together",
                                    icon = Icons.AutoMirrored.Outlined.MergeType,
                                    onClick = { onToolClick(PdfTool.MERGE) },
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
                                    onClick = { onToolClick(PdfTool.COMPRESS) },
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedToolCard(
                                    title = "Sign PDF",
                                    subtitle = "Add your signature",
                                    icon = Icons.Outlined.Draw,
                                    onClick = { onToolClick(PdfTool.SIGN_PDF) },
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
                                    onClick = { onToolClick(PdfTool.REORDER_PAGES) },
                                    modifier = Modifier.weight(1f)
                                )
                                FeaturedToolCard(
                                    title = "Lock PDF",
                                    subtitle = "Protect with password",
                                    icon = Icons.Outlined.Lock,
                                    onClick = { onToolClick(PdfTool.PROTECT) },
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
                                shape = RoundedCornerShape(18.dp),
                                color = if (isOled) colors.surface else colors.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Column {
                                    toolsInGroup.forEachIndexed { index, tool ->
                                        ToolListRow(
                                            tool = tool,
                                            onClick = { onToolClick(tool) }
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
 * Clean Material 3 Expressive Featured Tool Card.
 */
@Composable
private fun FeaturedToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = colors.accent,
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
    }
}
