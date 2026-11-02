package com.abhisheksati.mydoc.tools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel

private data class ToolNavCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    val label: String
)

/**
 * 6 primary task categories for tools:
 * Organize, Edit & Sign, Compress, Convert, Secure, Scan.
 */
private val toolCategories = listOf(
    ToolNavCategory(null, Icons.Outlined.AllInclusive, "All"),
    ToolNavCategory(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, "Organize"),
    ToolNavCategory(ToolGroup.EDIT_SIGN, Icons.Outlined.Draw, "Edit & Sign"),
    ToolNavCategory(ToolGroup.COMPRESS, Icons.Outlined.Compress, "Compress"),
    ToolNavCategory(ToolGroup.CONVERT, Icons.Outlined.Transform, "Convert"),
    ToolNavCategory(ToolGroup.SECURE, Icons.Outlined.Shield, "Secure"),
    ToolNavCategory(ToolGroup.SCAN, Icons.Outlined.CameraAlt, "Scan")
)

private fun friendlyHeader(group: ToolGroup): String = when (group) {
    ToolGroup.ORGANIZE -> "Organize"
    ToolGroup.EDIT_SIGN -> "Edit & Sign"
    ToolGroup.COMPRESS -> "Compress"
    ToolGroup.CONVERT -> "Convert"
    ToolGroup.SECURE -> "Secure"
    ToolGroup.SCAN -> "Scan"
}

/**
 * Editorial "Paper under glass" tool index:
 * - 29 PDF tools organized into 6 clear task categories.
 * - Stamp Vermilion accent indicators.
 * - Tactile paper card tool rows with monospace metadata.
 */
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onToolClick: (PdfTool) -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    val colors = AppTheme.colors

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var selectedGroup by rememberSaveable { mutableStateOf<ToolGroup?>(null) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) searchFocusRequester.requestFocus()
    }

    val allTools = PdfTool.entries
    val filteredTools = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            allTools
        } else {
            allTools.filter {
                it.title.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.group.title.lowercase().contains(q)
            }
        }
    }
    val groupedTools = remember(filteredTools) { filteredTools.groupBy { it.group } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        ToolsHeader(
            toolCount = allTools.size,
            isSearchActive = isSearchActive,
            onSearch = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) searchQuery = ""
            },
            onSettingsClick = onSettingsClick
        )

        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search tools…",
                        style = AppTheme.typography.body,
                        color = colors.inkMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = colors.inkSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = AppRadius.pillShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.inkPrimary,
                    unfocusedTextColor = colors.inkPrimary,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.inkMuted.copy(alpha = 0.3f),
                    cursorColor = colors.accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .focusRequester(searchFocusRequester)
            )
        }

        // Horizontal Category Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            toolCategories.forEach { category ->
                val selected = selectedGroup == category.group
                FilterChip(
                    selected = selected,
                    onClick = { selectedGroup = if (selected) null else category.group },
                    label = {
                        Text(
                            text = category.label,
                            style = AppTheme.typography.captionStrong
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent.copy(alpha = 0.14f),
                        selectedLabelColor = colors.accent,
                        selectedLeadingIconColor = colors.accent,
                        containerColor = colors.surface,
                        labelColor = colors.inkSecondary,
                        iconColor = colors.inkSecondary
                    ),
                    shape = AppRadius.pillShape
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            if (filteredTools.isEmpty()) {
                item(key = "empty") {
                    ToolsEmptyState(
                        onClear = {
                            searchQuery = ""
                            selectedGroup = null
                        }
                    )
                }
            } else {
                val groupsToDisplay = selectedGroup?.let { listOf(it) }
                    ?: ToolGroup.entries.filter { !groupedTools[it].isNullOrEmpty() }

                groupsToDisplay.forEach { group ->
                    val toolsInGroup = groupedTools[group].orEmpty()
                    if (toolsInGroup.isEmpty()) return@forEach

                    item(key = "header_${group.name}") {
                        Text(
                            text = friendlyHeader(group),
                            style = AppTheme.typography.heading,
                            color = colors.inkPrimary,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                        )
                    }

                    items(
                        count = toolsInGroup.size,
                        key = { index -> "tool_${toolsInGroup[index].name}" }
                    ) { index ->
                        ToolRow(
                            tool = toolsInGroup[index],
                            onClick = { onToolClick(toolsInGroup[index]) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsHeader(
    toolCount: Int,
    isSearchActive: Boolean,
    onSearch: () -> Unit,
    onSettingsClick: (() -> Unit)?
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Tools",
                style = AppTheme.typography.wordmark,
                color = colors.inkPrimary
            )
            Text(
                text = "$toolCount tools · 6 tasks",
                style = AppTheme.typography.metadata,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search tools",
                tint = if (isSearchActive) colors.accent else colors.inkSecondary
            )
        }

        if (onSettingsClick != null) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun ToolRow(
    tool: PdfTool,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(AppRadius.cardShape)
            .background(colors.surface)
            .border(
                width = 1.dp,
                color = colors.inkMuted.copy(alpha = 0.12f),
                shape = AppRadius.cardShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(AppRadius.insetShape)
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                style = AppTheme.typography.titleSmall,
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.description,
                style = AppTheme.typography.caption,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ToolsEmptyState(onClear: () -> Unit) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No tools found",
            style = AppTheme.typography.screenTitle,
            color = colors.inkPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Try a different action, or clear the filter.",
            style = AppTheme.typography.body,
            color = colors.inkMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClear) {
            Text(
                "Clear filters",
                style = AppTheme.typography.captionStrong,
                color = colors.accent
            )
        }
    }
}
