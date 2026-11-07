package com.abhisheksati.mydoc.tools.ui

import androidx.compose.runtime.setValue
import com.abhisheksati.mydoc.designsystem.components.LocalFloatingChromeInset
import com.abhisheksati.mydoc.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.GlassMotionState
import com.abhisheksati.mydoc.designsystem.components.TrackGlassMotion
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

private data class ToolNavCategory(
    val group: ToolGroup?,
    val icon: ImageVector,
    @StringRes val labelRes: Int
)

/**
 * 6 primary task categories for tools:
 * Organize, Edit & Sign, Compress, Convert, Secure, Scan.
 */
private val toolCategories = listOf(
    ToolNavCategory(null, Icons.Outlined.AllInclusive, R.string.tools_all_categories),
    ToolNavCategory(ToolGroup.ORGANIZE, Icons.AutoMirrored.Outlined.CallSplit, R.string.tools_filter_organize),
    ToolNavCategory(ToolGroup.EDIT_SIGN, Icons.Outlined.Draw, R.string.tools_filter_edit_sign),
    ToolNavCategory(ToolGroup.COMPRESS, Icons.Outlined.Compress, R.string.tools_filter_compress),
    ToolNavCategory(ToolGroup.CONVERT, Icons.Outlined.Transform, R.string.tools_filter_convert),
    ToolNavCategory(ToolGroup.SECURE, Icons.Outlined.Shield, R.string.tools_filter_secure),
    ToolNavCategory(ToolGroup.SCAN, Icons.Outlined.CameraAlt, R.string.tools_cat_scan)
)

private fun friendlyHeader(group: ToolGroup): Int = when (group) {
    ToolGroup.ORGANIZE -> R.string.tools_filter_organize
    ToolGroup.EDIT_SIGN -> R.string.tools_filter_edit_sign
    ToolGroup.COMPRESS -> R.string.tools_filter_compress
    ToolGroup.CONVERT -> R.string.tools_filter_convert
    ToolGroup.SECURE -> R.string.tools_filter_secure
    ToolGroup.SCAN -> R.string.tools_cat_scan
}

/**
 * Editorial "Ink & Seal" tool index:
 * - 24 PDF tools organized into 6 clear task categories.
 * - Seal teal accent indicators.
 * - Tactile paper card tool rows with monospace metadata.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onToolClick: (PdfTool) -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    glassMotion: GlassMotionState? = null
) {
    val colors = AppTheme.colors
    val listState = rememberLazyListState()
    if (glassMotion != null) {
        TrackGlassMotion(listState, glassMotion)
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedGroup by rememberSaveable { mutableStateOf<ToolGroup?>(null) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) searchFocusRequester.requestFocus()
    }

    val allTools = PdfTool.entries
    val searchable = localizedToolLabels(allTools)
    val filteredTools = remember(searchQuery, searchable) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            allTools
        } else {
            allTools.filter { tool ->
                val (title, description) = searchable.getValue(tool)
                title.lowercase().contains(q) ||
                    description.lowercase().contains(q) ||
                    tool.group.title.lowercase().contains(q)
            }
        }
    }
    val visibleTools = remember(filteredTools, selectedGroup) {
        filteredTools.filter { selectedGroup == null || it.group == selectedGroup }
    }
    val groupedTools = remember(visibleTools) { visibleTools.groupBy { it.group } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
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
            GlassSurface(
                shape = AppRadius.pillShape,
                elevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                    contentDescription = stringResource(R.string.cd_clear_search),
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
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = colors.accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                )
            }
        }

        // Keep every category one tap away without pushing tools below three rows of chips.
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            val activeCategory = toolCategories.first { it.group == selectedGroup }
            OutlinedButton(onClick = { categoryMenuExpanded = true }, shape = AppRadius.pillShape) {
                Icon(activeCategory.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(activeCategory.labelRes), modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Outlined.ExpandMore, contentDescription = null)
            }
            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                toolCategories.forEach { category ->
                    DropdownMenuItem(
                        modifier = Modifier.semantics { selected = selectedGroup == category.group },
                        text = { Text(stringResource(category.labelRes)) },
                        leadingIcon = { Icon(category.icon, contentDescription = null) },
                        trailingIcon = {
                            if (selectedGroup == category.group) Icon(Icons.Outlined.Check, contentDescription = null)
                        },
                        onClick = {
                            selectedGroup = category.group
                            categoryMenuExpanded = false
                        }
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 8.dp, bottom = LocalFloatingChromeInset.current + 24.dp)
        ) {
            if (visibleTools.isEmpty()) {
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
                            text = stringResource(friendlyHeader(group)),
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
    val scheme = MaterialTheme.colorScheme
    AppTopBar(
        title = stringResource(R.string.nav_tools),
        subtitle = stringResource(R.string.tools_subtitle, toolCount),
        isGlass = false,
        containerColor = Color.Transparent,
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.cd_search_tools),
                    tint = if (isSearchActive) scheme.primary else scheme.onSurface
                )
            }
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = scheme.onSurface
                    )
                }
            }
        }
    )
}

@Composable
private fun ToolRow(
    tool: PdfTool,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    GlassSurface(
        shape = AppRadius.cardShape,
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(AppRadius.cardShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    text = stringResource(tool.titleRes),
                    style = AppTheme.typography.bodyLargeStrong,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(tool.descriptionRes),
                    style = AppTheme.typography.caption,
                    color = colors.inkMuted,
                    maxLines = 2,
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
            text = stringResource(R.string.tools_none_found),
            style = AppTheme.typography.screenTitle,
            color = colors.inkPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tools_none_hint),
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

/** Titles and descriptions resolved for search; stringResource is composable-only. */
@Composable
private fun localizedToolLabels(tools: List<PdfTool>): Map<PdfTool, Pair<String, String>> {
    val labels = LinkedHashMap<PdfTool, Pair<String, String>>()
    for (tool in tools) {
        labels[tool] = stringResource(tool.titleRes) to stringResource(tool.descriptionRes)
    }
    return labels
}
