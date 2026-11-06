package com.abhisheksati.mydoc.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.R
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
import android.provider.DocumentsContract
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import com.abhisheksati.mydoc.designsystem.components.SealMark
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
 * Editorial "Ink & Seal" Home feed:
 * - Wordmark header with Fraunces serif
 * - "Continue Reading" Hero paper card with real page thumbnail & Seal teal progress
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
    selectedRecentUris: Set<String>,
    onToggleRecentSelection: (LibraryItem) -> Unit,
    onSelectAllRecents: (List<LibraryItem>) -> Unit,
    onClearRecentSelection: () -> Unit,
    onShareSelectedRecents: (List<LibraryItem>) -> Unit,
    onRemoveSelectedRecents: (List<LibraryItem>) -> Unit,
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
    val pickerContext = LocalContext.current

    // Read in composition: the picker callback is not composable, so it needs the value hoisted.
    val documentFallbackName = stringResource(R.string.home_document_fallback)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // The picker takes */*, so the picked file may be anything. Reading its real type,
            // name and size keeps the icon, badge and viewer routing honest.
            val mime = runCatching { pickerContext.contentResolver.getType(uri) }.getOrNull()
            var displayName = uri.lastPathSegment?.substringAfterLast('/', "")
            var sizeBytes = 0L
            var lastModified = System.currentTimeMillis()
            runCatching {
                val columns = arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )
                pickerContext.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(0)?.let { if (it.isNotBlank()) displayName = it }
                        if (!cursor.isNull(1)) sizeBytes = cursor.getLong(1)
                        if (!cursor.isNull(2)) lastModified = cursor.getLong(2)
                    }
                }
            }
            val kind = when {
                mime?.startsWith("image/") == true -> MediaKind.PHOTO
                mime?.startsWith("video/") == true -> MediaKind.VIDEO
                mime?.startsWith("audio/") == true -> MediaKind.AUDIO
                else -> MediaKind.DOCUMENT
            }
            onLibraryItemClick(
                LibraryItem(
                    uri = uri.toString(),
                    name = displayName?.takeIf { it.isNotBlank() } ?: documentFallbackName,
                    kind = kind,
                    mimeType = mime ?: "application/octet-stream",
                    sizeBytes = sizeBytes,
                    lastModified = lastModified
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

    val isRecentsSelectionActive = selectedRecentUris.isNotEmpty()
    val selectedRecents = remainingRecents.filter { it.uri in selectedRecentUris }

    val scrollModifier = if (hazeState != null) {
        Modifier.hazeSource(state = hazeState)
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                        // Brand lockup: Seal + wordmark, per docs/brand-identity.md §3.
                        SealMark(modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
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
                                contentDescription = stringResource(R.string.cd_search_documents),
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
                                contentDescription = stringResource(R.string.cd_settings),
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
                                        contentDescription = stringResource(R.string.cd_dismiss),
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
                                text = stringResource(R.string.home_recent_documents),
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
                                text = stringResource(R.string.home_view_all),
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
                                    isSelected = docItem.uri in selectedRecentUris,
                                    isSelectionMode = isRecentsSelectionActive,
                                    onClick = {
                                        haptics.click()
                                        if (isRecentsSelectionActive) {
                                            onToggleRecentSelection(docItem)
                                        } else {
                                            onLibraryItemClick(docItem)
                                        }
                                    },
                                    onLongClick = {
                                        haptics.snap()
                                        onToggleRecentSelection(docItem)
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

        AnimatedVisibility(
            visible = isRecentsSelectionActive,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            HomeRecentsSelectionBar(
                selectedCount = selectedRecents.size,
                totalCount = remainingRecents.size,
                onClose = onClearRecentSelection,
                onSelectAll = { onSelectAllRecents(remainingRecents) },
                onShare = { onShareSelectedRecents(selectedRecents) },
                onRemove = { onRemoveSelectedRecents(selectedRecents) }
            )
        }
    }
}

/**
 * Hero Card: Real rendered page thumbnail + Seal teal progress bar + Fraunces title.
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
                    text = stringResource(R.string.home_continue_reading),
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
                            text = stringResource(R.string.home_resume),
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentPaperCard(
    item: LibraryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.accent.copy(alpha = 0.18f))
                    )
                }

                // In selection mode the check replaces the per-card menu; the two compete for
                // the same corner and only one gesture set should be live at a time.
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = stringResource(if (isSelected) R.string.cd_selected else R.string.cd_not_selected),
                        tint = if (isSelected) colors.accent else colors.inkSecondary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .background(colors.surface, CircleShape)
                    )
                } else {
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
                                contentDescription = stringResource(R.string.cd_options),
                                tint = colors.inkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AppDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_details), style = AppTheme.typography.body) },
                                onClick = {
                                    menuExpanded = false
                                    onDetails()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_share), style = AppTheme.typography.body) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_remove_recent), style = AppTheme.typography.body) },
                                onClick = {
                                    menuExpanded = false
                                    onRemove()
                                }
                            )
                        }
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
                    text = stringResource(R.string.home_empty_title),
                    style = AppTheme.typography.title,
                    color = colors.ink
                )
                Text(
                    text = stringResource(R.string.home_empty_message),
                    style = AppTheme.typography.body,
                    color = colors.inkSecondary
                )
            }

            // 3 Quick Action Tiles
            ActionTile(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.home_action_open_title),
                subtitle = stringResource(R.string.home_action_open_subtitle),
                onClick = onOpenFile
            )

            ActionTile(
                icon = Icons.Outlined.CameraAlt,
                title = stringResource(R.string.home_action_scan_title),
                subtitle = stringResource(R.string.home_action_scan_subtitle),
                onClick = onScan
            )

            ActionTile(
                icon = Icons.Outlined.FolderOpen,
                title = stringResource(R.string.home_action_browse_title),
                subtitle = stringResource(R.string.home_action_browse_subtitle),
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

/**
 * Bulk-action bar for the Home recents grid: the same set of actions the Library offers for a
 * multi-selection, narrowed to the two that mean something for a recents list.
 */
@Composable
private fun HomeRecentsSelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    GlassSurface(
        shape = AppRadius.cardShape,
        elevation = 6.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(
                start = AppSpacing.medium,
                end = AppSpacing.medium,
                bottom = AppSpacing.navBarHeight + AppSpacing.small
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_close_selection),
                    tint = colors.ink
                )
            }
            Text(
                text = "$selectedCount selected",
                style = typography.bodyStrong,
                color = colors.ink,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (selectedCount < totalCount) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = Icons.Outlined.SelectAll,
                        contentDescription = stringResource(R.string.cd_select_all),
                        tint = colors.ink
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.cd_share_selected),
                    tint = colors.ink
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_remove_selected),
                    tint = colors.error
                )
            }
        }
    }
}
