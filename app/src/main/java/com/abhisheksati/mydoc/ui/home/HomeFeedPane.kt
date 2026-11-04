package com.abhisheksati.mydoc.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.HomeFeedSkeleton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.documentHero
import com.abhisheksati.mydoc.designsystem.animation.documentHeroKey
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.ui.files.LibraryThumbnail
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Editorial "Paper under glass" Home feed:
 * - Wordmark header with Fraunces serif
 * - "Continue Reading" Hero paper card with real page thumbnail & Stamp Vermilion progress
 * - 2-column Recent Documents paper cards grid with monospace metadata
 * - Curated first-run state with common quick actions
 */
@Composable
fun HomeFeedPane(
    continueItems: List<LibraryItem>,
    recentItems: List<LibraryItem>,
    onLibraryItemClick: (LibraryItem) -> Unit,
    onLibraryItemDetails: (LibraryItem) -> Unit,
    onRemoveFromRecents: (LibraryItem) -> Unit,
    onShareDocument: (LibraryItem) -> Unit,
    onSpotlightSearch: () -> Unit,
    onSettingsClick: () -> Unit,
    onScanClick: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onDismissError: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    listState: LazyListState = rememberLazyListState()
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onLibraryItemClick(
                LibraryItem(
                    uri = uri.toString(),
                    name = uri.lastPathSegment ?: "Document",
                    kind = MediaKind.DOCUMENT,
                    mimeType = "application/pdf",
                    sizeBytes = 0L,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    val heroItem = continueItems.firstOrNull() ?: recentItems.firstOrNull()
    val remainingRecents = if (heroItem != null) {
        recentItems.filter { it.uri != heroItem.uri }
    } else {
        recentItems
    }

    val scrollModifier = if (hazeState != null) {
        modifier.hazeSource(state = hazeState)
    } else {
        modifier
    }

    LazyColumn(
        state = listState,
        modifier = scrollModifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppSpacing.medium,
            end = AppSpacing.medium,
            top = 0.dp,
            bottom = 96.dp // Ample space for floating glass bottom navigation
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        // TOP EDITORIAL HEADER
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MyDoc",
                        style = AppTheme.typography.display,
                        color = colors.ink
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            haptics.tick()
                            onSpotlightSearch()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search documents",
                            tint = colors.ink
                        )
                    }
                    IconButton(
                        onClick = {
                            haptics.tick()
                            onSettingsClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colors.ink
                        )
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            item(key = "home_error_banner") {
                GlassSurface(
                    shape = AppRadius.cardShape,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (onDismissError != null) {
                            IconButton(onClick = onDismissError) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // CONTINUE READING HERO CARD
        if (isLoading) {
            item(key = "feed_skeleton") {
                HomeFeedSkeleton()
            }
        } else if (heroItem != null) {
            item(key = "hero_continue_reading") {
                ContinueReadingHeroCard(
                    item = heroItem,
                    onClick = {
                        haptics.click()
                        onLibraryItemClick(heroItem)
                    }
                )
            }
        } else {
            // CURATED FIRST-RUN EMPTY STATE
            item(key = "first_run_empty_state") {
                FirstRunPaperCard(
                    onOpenFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onScan = { onScanClick() },
                    onBrowse = { onNavigateToLibrary() }
                )
            }
        }

        // RECENT DOCUMENTS SECTION
        if (remainingRecents.isNotEmpty()) {
            item(key = "recents_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recent Documents",
                            style = AppTheme.typography.heading,
                            color = colors.ink
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceMuted)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${remainingRecents.size}",
                                style = AppTheme.typography.metadata,
                                color = colors.inkSecondary
                            )
                        }
                    }

                    TextButton(onClick = onNavigateToLibrary) {
                        Text(
                            text = "View all",
                            style = AppTheme.typography.captionStrong,
                            color = colors.accent
                        )
                    }
                }
            }

            // 2-Column Grid of Paper Cards (chunked in pairs)
            val chunkedRecents = remainingRecents.chunked(2)
            items(
                items = chunkedRecents,
                key = { chunk -> chunk.joinToString(",") { it.uri } }
            ) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    pair.forEach { docItem ->
                        Box(modifier = Modifier.weight(1f)) {
                            RecentPaperCard(
                                item = docItem,
                                onClick = {
                                    haptics.click()
                                    onLibraryItemClick(docItem)
                                },
                                onDetails = { onLibraryItemDetails(docItem) },
                                onShare = { onShareDocument(docItem) },
                                onRemove = { onRemoveFromRecents(docItem) }
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Hero Card: Real rendered page thumbnail + Stamp Vermilion progress bar + Fraunces title.
 */
@Composable
private fun ContinueReadingHeroCard(
    item: LibraryItem,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    GlassSurface(
        shape = RoundedCornerShape(16.dp),
        elevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real document page thumbnail with shared-element transition
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 104.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceMuted)
                    .border(width = 1.dp, color = colors.cardBorder, shape = RoundedCornerShape(8.dp))
                    .documentHero(documentHeroKey(item.uri))
            ) {
                LibraryThumbnail(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Document info and progress
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "CONTINUE READING",
                    style = AppTheme.typography.overline,
                    color = colors.accent
                )

                Text(
                    text = item.name,
                    style = AppTheme.typography.titleSmall,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata row in JetBrains Mono
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.pageCount > 0) {
                        Text(
                            text = "p. ${item.lastPage + 1}/${item.pageCount}",
                            style = AppTheme.typography.metadata,
                            color = colors.inkSecondary
                        )
                        Text(
                            text = "•",
                            style = AppTheme.typography.metadata,
                            color = colors.cardBorder
                        )
                    }
                    Text(
                        text = item.formattedSize,
                        style = AppTheme.typography.metadata,
                        color = colors.inkSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Indicator
                val progress = if (item.pageCount > 0) {
                    ((item.lastPage + 1).toFloat() / item.pageCount).coerceIn(0.05f, 1f)
                } else {
                    item.progressFraction.coerceIn(0f, 1f)
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = colors.accent,
                    trackColor = colors.surfaceMuted
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.formattedLastModified,
                        style = AppTheme.typography.metadata,
                        color = colors.inkMuted
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Resume",
                            style = AppTheme.typography.captionStrong,
                            color = colors.accent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2-Column Recent Paper Card:
 * Tactile paper surface + real thumbnail + Fraunces title + JetBrains Mono metadata.
 */
@Composable
private fun RecentPaperCard(
    item: LibraryItem,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = AppTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    GlassSurface(
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Document thumbnail preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .background(colors.surfaceMuted)
                    .documentHero(documentHeroKey(item.uri))
            ) {
                LibraryThumbnail(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )

                // More options button (top right overlay)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AppDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Details", style = AppTheme.typography.body) },
                            onClick = {
                                menuExpanded = false
                                onDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share", style = AppTheme.typography.body) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from recents", style = AppTheme.typography.body) },
                            onClick = {
                                menuExpanded = false
                                onRemove()
                            }
                        )
                    }
                }
            }

            // Paper card details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.name,
                    style = AppTheme.typography.documentName,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.formattedSize,
                        style = AppTheme.typography.metadata,
                        color = colors.inkSecondary
                    )
                    Text(
                        text = item.formattedLastModified,
                        style = AppTheme.typography.metadata,
                        color = colors.inkMuted
                    )
                }

                // Thin reading progress indicator if in-progress
                if (item.pageCount > 0 && item.lastPage > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { ((item.lastPage + 1).toFloat() / item.pageCount).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(CircleShape),
                        color = colors.accent,
                        trackColor = colors.surfaceMuted
                    )
                }
            }
        }
    }
}

/**
 * First-run curated paper card when the user has no recent files.
 */
@Composable
private fun FirstRunPaperCard(
    onOpenFile: () -> Unit,
    onScan: () -> Unit,
    onBrowse: () -> Unit
) {
    val colors = AppTheme.colors

    GlassSurface(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Your documents, under glass.",
                    style = AppTheme.typography.title,
                    color = colors.ink
                )
                Text(
                    text = "Open any PDF, Office doc, or book to start reading with crisp typography and zero clutter.",
                    style = AppTheme.typography.body,
                    color = colors.inkSecondary
                )
            }

            // 3 Quick Action Tiles
            ActionTile(
                icon = Icons.Outlined.Description,
                title = "Open document",
                subtitle = "Select any file from device storage",
                onClick = onOpenFile
            )

            ActionTile(
                icon = Icons.Outlined.CameraAlt,
                title = "Scan with camera",
                subtitle = "Turn physical papers into crisp PDFs",
                onClick = onScan
            )

            ActionTile(
                icon = Icons.Outlined.FolderOpen,
                title = "Browse library",
                subtitle = "Explore device folders and storage",
                onClick = onBrowse
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    GlassSurface(
        shape = RoundedCornerShape(10.dp),
        elevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.typography.bodyStrong,
                color = colors.ink
            )
            Text(
                text = subtitle,
                style = AppTheme.typography.caption,
                color = colors.inkSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.size(16.dp)
        )
        }
    }
}
