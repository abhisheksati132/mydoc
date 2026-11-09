package com.abhisheksati.mydoc.ui.viewer.controls

import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.rendering.engine.OutlineItem

private enum class DrawerTab {
    OUTLINE,
    BOOKMARKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineDrawer(
    outline: List<OutlineItem>,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    bookmarks: Set<Int> = emptySet(),
    onRemoveBookmark: (pageIndex: Int) -> Unit = {}
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(if (outline.isNotEmpty()) DrawerTab.OUTLINE else DrawerTab.BOOKMARKS) }

    AppModalSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        OverlaySystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.medium)
        ) {
            // Tabs: Outline vs Bookmarks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                FilterChip(
                    selected = selectedTab == DrawerTab.OUTLINE,
                    onClick = { selectedTab = DrawerTab.OUTLINE },
                    label = { Text(stringResource(R.string.outline_title)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                FilterChip(
                    selected = selectedTab == DrawerTab.BOOKMARKS,
                    onClick = { selectedTab = DrawerTab.BOOKMARKS },
                    label = { Text(stringResource(R.string.outline_bookmarks, bookmarks.size)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (selectedTab) {
                DrawerTab.OUTLINE -> {
                    val flattened = flattenOutline(outline)
                    if (flattened.isEmpty()) {
                        Text(
                            text = stringResource(R.string.outline_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = AppSpacing.large)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            items(flattened) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onItemSelected(item.pageIndex)
                                            onDismiss()
                                        }
                                        .heightIn(min = 48.dp)
                                        .padding(vertical = AppSpacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width((item.level * 16).dp))
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(AppSpacing.small))
                                    Text(
                                        text = "${item.pageIndex + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }

                DrawerTab.BOOKMARKS -> {
                    val sortedBookmarks = remember(bookmarks) { bookmarks.toList().sorted() }
                    if (sortedBookmarks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.bookmarks_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = AppSpacing.large)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            items(sortedBookmarks) { pageIndex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onItemSelected(pageIndex)
                                            onDismiss()
                                        }
                                        .heightIn(min = 48.dp)
                                        .padding(vertical = AppSpacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Bookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(AppSpacing.small))
                                    Text(
                                        text = stringResource(R.string.page_label, pageIndex + 1),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onRemoveBookmark(pageIndex) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = stringResource(R.string.cd_remove_bookmark),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun flattenOutline(items: List<OutlineItem>): List<OutlineItem> {
    val list = mutableListOf<OutlineItem>()
    fun traverse(node: OutlineItem) {
        list.add(node)
        for (child in node.children) {
            traverse(child)
        }
    }
    for (item in items) {
        traverse(item)
    }
    return list
}
