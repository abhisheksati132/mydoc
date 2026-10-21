package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing as Spacing

private enum class DrawerTab {
    OUTLINE,
    BOOKMARKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineDrawer(
    outline: List<OutlineItem>,
    bookmarks: Set<Int> = emptySet(),
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (pageIndex: Int) -> Unit,
    onRemoveBookmark: (pageIndex: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(if (outline.isNotEmpty()) DrawerTab.OUTLINE else DrawerTab.BOOKMARKS) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
        ) {
            // Tabs: Outline vs Bookmarks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                FilterChip(
                    selected = selectedTab == DrawerTab.OUTLINE,
                    onClick = { selectedTab = DrawerTab.OUTLINE },
                    label = { Text("Outline") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(6.dp)
                )

                FilterChip(
                    selected = selectedTab == DrawerTab.BOOKMARKS,
                    onClick = { selectedTab = DrawerTab.BOOKMARKS },
                    label = { Text("Bookmarks (${bookmarks.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))
            AppDivider()

            when (selectedTab) {
                DrawerTab.OUTLINE -> {
                    val flattened = flattenOutline(outline)
                    if (flattened.isEmpty()) {
                        Text(
                            text = "This document does not contain an outline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.large)
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
                                        .padding(vertical = Spacing.small),
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
                                    Spacer(modifier = Modifier.width(Spacing.small))
                                    Text(
                                        text = "${item.pageIndex + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AppDivider()
                            }
                        }
                    }
                }

                DrawerTab.BOOKMARKS -> {
                    val sortedBookmarks = remember(bookmarks) { bookmarks.toList().sorted() }
                    if (sortedBookmarks.isEmpty()) {
                        Text(
                            text = "No bookmarks yet. Tap the bookmark icon in the top toolbar to save a page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.large)
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
                                        .padding(vertical = Spacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Bookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.small))
                                    Text(
                                        text = "Page ${pageIndex + 1}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onRemoveBookmark(pageIndex) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove Bookmark",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                AppDivider()
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
