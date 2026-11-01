package com.abhisheksati.mydoc.tools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.AppChip
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.components.AuroraBackdrop
import com.abhisheksati.mydoc.designsystem.components.VividIconTile
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsDark
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.VividGradients
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel

private data class ToolNavCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    val label: String
)

private fun friendlyHeader(group: ToolGroup): String = when (group) {
    ToolGroup.ORGANIZE -> "Organize & pages"
    ToolGroup.OPTIMIZE -> "Compress & optimize"
    ToolGroup.CONVERT_TO -> "Convert to PDF"
    ToolGroup.CONVERT_FROM -> "Convert from PDF"
    ToolGroup.EDIT -> "Sign & annotate"
    ToolGroup.SECURITY -> "Privacy & protection"
    ToolGroup.SCAN -> "Camera & OCR"
}

/**
 * The tool index: soft cards with a gradient plaque per tool, hue-coded by what
 * the tool does. The chip row filters; the aurora canvas ties it to the home page.
 */
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onToolClick: (PdfTool) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val paletteKey = if (LocalIsDark.current) if (LocalIsOled.current) "b" else "d" else "l"

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AuroraBackdrop(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
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
                onToggleTheme = onToggleTheme,
                onSettingsClick = onSettingsClick,
                bottomContent = {
                    ToolGroupChips(
                        selected = selectedToolGroup,
                        onSelected = { group -> selectedToolGroup = group }
                    )
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
                    DropdownMenuItem(
                        text = { Text("Merge Documents", style = typography.body) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.MergeType, contentDescription = null, tint = colors.accent)
                        },
                        onClick = {
                            onDismiss()
                            onToolClick(PdfTool.MERGE)
                        }
                    )
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = AppSpacing.large)
            ) {
                if (filteredTools.isEmpty()) {
                    item(key = "empty_$paletteKey") {
                        AppEmptyState(
                            title = "No tools found",
                            message = "Try searching for a different action, or clear the filter.",
                            actionLabel = "Clear filter",
                            onAction = {
                                searchQuery = ""
                                selectedToolGroup = null
                            }
                        )
                    }
                } else {
                    val groupsToDisplay = if (selectedToolGroup != null) {
                        listOf(selectedToolGroup!!)
                    } else {
                        ToolGroup.entries.filter { !groupedTools[it].isNullOrEmpty() }
                    }

                    groupsToDisplay.forEach { group ->
                        val toolsInGroup = groupedTools[group].orEmpty()
                        if (toolsInGroup.isEmpty()) return@forEach

                        item(key = "header_${group.name}_$paletteKey") {
                            AppSectionHeader(title = friendlyHeader(group))
                        }

                        items(
                            count = toolsInGroup.size,
                            key = { index -> "tool_${toolsInGroup[index].name}_$paletteKey" }
                        ) { index ->
                            ToolCard(
                                tool = toolsInGroup[index],
                                onClick = { onToolClick(toolsInGroup[index]) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One tool: soft card, gradient plaque hue-coded by group, quiet chevron. */
@Composable
private fun ToolCard(
    tool: PdfTool,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val shape = AppRadius.cardShape

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                bottom = AppSpacing.small
            )
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .pressScaleEffect(onClick = onClick)
            .padding(AppSpacing.small + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        VividIconTile(
            icon = tool.icon,
            family = familyForGroup(tool.group),
            size = 40.dp,
            iconSize = 20.dp,
            shape = shape
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                style = typography.bodyStrong,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.description,
                style = typography.caption,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun familyForGroup(group: ToolGroup): VividGradients.Family = when (group) {
    ToolGroup.ORGANIZE -> VividGradients.Family.OCEAN
    ToolGroup.OPTIMIZE -> VividGradients.Family.FOREST
    ToolGroup.CONVERT_TO -> VividGradients.Family.GRAPE
    ToolGroup.CONVERT_FROM -> VividGradients.Family.SUNSET
    ToolGroup.EDIT -> VividGradients.Family.GOLD
    ToolGroup.SECURITY -> VividGradients.Family.FIRE
    ToolGroup.SCAN -> VividGradients.Family.BRAND
}

@Composable
private fun ToolGroupChips(
    selected: ToolGroup?,
    onSelected: (ToolGroup?) -> Unit
) {
    val categories = listOf(
        ToolNavCategory(null, Icons.Outlined.AllInclusive, "All"),
        ToolNavCategory(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, "Organize"),
        ToolNavCategory(ToolGroup.OPTIMIZE, Icons.Outlined.Compress, "Optimize"),
        ToolNavCategory(ToolGroup.CONVERT_TO, Icons.Outlined.Transform, "Convert to"),
        ToolNavCategory(ToolGroup.CONVERT_FROM, Icons.Outlined.Transform, "Convert from"),
        ToolNavCategory(ToolGroup.EDIT, Icons.Outlined.Edit, "Edit"),
        ToolNavCategory(ToolGroup.SECURITY, Icons.Outlined.Security, "Security"),
        ToolNavCategory(ToolGroup.SCAN, Icons.Outlined.CameraAlt, "Scan")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScrollWithFade(rememberScrollState())
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.small,
                bottom = AppSpacing.small
            ),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        categories.forEach { category ->
            AppChip(
                label = category.label,
                selected = selected == category.group,
                onClick = {
                    onSelected(if (selected == category.group) null else category.group)
                },
                leadingIcon = category.icon
            )
        }
    }
}
