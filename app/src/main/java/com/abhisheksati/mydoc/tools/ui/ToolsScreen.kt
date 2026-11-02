package com.abhisheksati.mydoc.tools.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel

private data class ToolNavCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    val label: String
)

private val toolCategories = listOf(
    ToolNavCategory(null, Icons.Outlined.AllInclusive, "All"),
    ToolNavCategory(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, "Organize"),
    ToolNavCategory(ToolGroup.OPTIMIZE, Icons.Outlined.Compress, "Optimize"),
    ToolNavCategory(ToolGroup.CONVERT_TO, Icons.Outlined.Transform, "Convert to"),
    ToolNavCategory(ToolGroup.CONVERT_FROM, Icons.Outlined.Transform, "Convert from"),
    ToolNavCategory(ToolGroup.EDIT, Icons.Outlined.Edit, "Edit"),
    ToolNavCategory(ToolGroup.SECURITY, Icons.Outlined.Security, "Security"),
    ToolNavCategory(ToolGroup.SCAN, Icons.Outlined.CameraAlt, "Scan")
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
 * The tool index: one searchable, grouped list of PDF actions on the same
 * Material 3 surface language as the Files screen.
 */
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onToolClick: (PdfTool) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

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
            .background(scheme.background)
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
                placeholder = { Text("Search tools") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = AppRadius.pillShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(searchFocusRequester)
            )
        }

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
                    label = { Text(category.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
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
                            style = typography.titleMedium,
                            color = scheme.onBackground,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
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
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = "Tools",
                style = typography.headlineSmall,
                color = scheme.onBackground
            )
            Text(
                text = "$toolCount PDF tools",
                style = typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search tools",
                tint = if (isSearchActive) scheme.primary else scheme.onSurface
            )
        }

        if (onSettingsClick != null) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = scheme.onSurface)
            }
        }
    }
}

@Composable
private fun ToolRow(
    tool: PdfTool,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(AppRadius.cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(AppRadius.insetShape)
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                style = typography.bodyLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.description,
                style = typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ToolsEmptyState(onClear: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

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
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No tools found",
            style = typography.titleMedium,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Try a different action, or clear the filter.",
            style = typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClear) { Text("Clear filters") }
    }
}
