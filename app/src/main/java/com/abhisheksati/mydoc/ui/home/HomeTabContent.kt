package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.animation.documentHero
import com.abhisheksati.mydoc.designsystem.animation.documentHeroKey
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.components.DocumentFilterChips
import com.abhisheksati.mydoc.designsystem.components.EditorialAction
import com.abhisheksati.mydoc.designsystem.components.EditorialRow
import com.abhisheksati.mydoc.designsystem.components.EditorialRule
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.tools.model.PdfTool

/**
 * The home page, set like a printed index:
 * masthead and rule, a four-cell action band, folio 01 for the document in progress,
 * folio 02 for the recent list. Everything below the masthead is rules, type and
 * white space; there are no cards, tiles or coloured containers on this page.
 */
@Composable
fun HomeTabContent(
    continueItems: List<LibraryItem>,
    recentItems: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onToolClick: (PdfTool) -> Unit = {},
    onSettingsClick: () -> Unit,
    onToggleTheme: () -> Unit = {},
    onClearRecent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    var showClearRecentDialog by remember { mutableStateOf(false) }
    var selectedDocumentFilter by remember { mutableStateOf(DocumentFilter.ALL) }

    val filteredRecentItems = remember(recentItems, selectedDocumentFilter) {
        recentItems.filter { selectedDocumentFilter.matches(it.extension) }
    }
    val continueItem = continueItems.firstOrNull()

    if (showClearRecentDialog) {
        AppDialog(
            isOpen = true,
            title = stringResource(R.string.clear_recent_history),
            message = stringResource(R.string.clear_recent_history_desc),
            confirmLabel = stringResource(R.string.clear_cache),
            dismissLabel = "Cancel",
            onConfirm = {
                showClearRecentDialog = false
                onClearRecent()
            },
            onDismiss = { showClearRecentDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AppTabHeader(
            title = "MyDoc",
            titleContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "My",
                        style = typography.wordmark,
                        color = colors.inkMuted
                    )
                    Text(
                        text = "Doc",
                        style = typography.wordmark,
                        color = colors.accent
                    )
                }
            },
            onSearchClick = onSearchClick,
            onToggleTheme = onToggleTheme,
            onSettingsClick = onSettingsClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppSpacing.large)
        ) {
            item(key = "action_band") {
                ActionBand(
                    onScanClick = onScanClick,
                    onToolClick = onToolClick
                )
            }

            if (continueItem != null) {
                item(key = "continue_header") {
                    AppSectionHeader(
                        title = "Continue reading",
                        index = "01",
                        showRule = false
                    )
                }
                item(key = "continue_row") {
                    ContinueReadingRow(
                        item = continueItem,
                        onClick = { onItemClick(continueItem) }
                    )
                }
            }

            item(key = "recent_header") {
                AppSectionHeader(
                    title = "Recent documents",
                    index = if (continueItem != null) "02" else "01",
                    showRule = false,
                    action = if (recentItems.isNotEmpty()) {
                        {
                            EditorialAction(
                                label = "Clear",
                                onClick = { showClearRecentDialog = true }
                            )
                        }
                    } else null
                )
            }

            item(key = "filter_chips") {
                DocumentFilterChips(
                    selected = selectedDocumentFilter,
                    onSelected = { selectedDocumentFilter = it },
                    modifier = Modifier.padding(
                        start = AppSpacing.screenPadding,
                        end = AppSpacing.screenPadding,
                        bottom = AppSpacing.small
                    )
                )
            }

            if (filteredRecentItems.isEmpty()) {
                item(key = "empty_recents") {
                    if (selectedDocumentFilter == DocumentFilter.ALL) {
                        AppEmptyState(
                            title = "Nothing here yet",
                            message = "Scan a page or open a file and it will be listed here.",
                            actionLabel = "Scan a document",
                            onActionClick = onScanClick
                        )
                    } else {
                        AppEmptyState(
                            title = "No ${selectedDocumentFilter.label} files",
                            message = "This page of the index is empty.",
                            actionLabel = "Show all",
                            onActionClick = { selectedDocumentFilter = DocumentFilter.ALL }
                        )
                    }
                }
            } else {
                items(
                    count = filteredRecentItems.size,
                    key = { index -> filteredRecentItems[index].uri }
                ) { index ->
                    val item = filteredRecentItems[index]
                    EditorialRow(
                        title = formatMiddleTruncated(item.name, 40),
                        meta = itemMeta(item),
                        leading = {
                            Icon(
                                imageVector = getDocumentFormatIcon(item.extension),
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = colors.inkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = { onItemClick(item) },
                        divider = index < filteredRecentItems.lastIndex,
                        modifier = if (item.uri != continueItem?.uri) {
                            Modifier.documentHero(documentHeroKey(item.uri))
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}

/**
 * Four fixed cells, divided by hairlines and closed by a full-bleed rule.
 * Every action is visible at once: nothing scrolls sideways and nothing is clipped.
 */
@Composable
private fun ActionBand(
    onScanClick: () -> Unit,
    onToolClick: (PdfTool) -> Unit
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppSpacing.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSpacing.objectRowHeight + 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionCell(
                label = "Scan",
                icon = Icons.Outlined.CameraAlt,
                primary = true,
                onClick = onScanClick
            )
            ActionDivider()
            ActionCell(
                label = "Merge",
                icon = Icons.AutoMirrored.Outlined.MergeType,
                onClick = { onToolClick(PdfTool.MERGE) }
            )
            ActionDivider()
            ActionCell(
                label = "Compress",
                icon = Icons.Outlined.Compress,
                onClick = { onToolClick(PdfTool.COMPRESS) }
            )
            ActionDivider()
            ActionCell(
                label = "Sign",
                icon = Icons.Outlined.Draw,
                onClick = { onToolClick(PdfTool.SIGN_PDF) }
            )
        }
        EditorialRule(thickness = AppSpacing.hairline, color = colors.line)
    }
}

@Composable
private fun RowScope.ActionCell(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pressScaleEffect(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) colors.accent else colors.ink,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Text(
            text = label,
            style = typography.captionStrong,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionDivider() {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .width(AppSpacing.hairline)
            .height(AppSpacing.large)
            .background(colors.line)
    )
}

/**
 * The document in progress: one ruled row, a hanging glyph, and a two-stroke gauge
 * that shows how much is left. The gauge is the only accent on the page.
 */
@Composable
private fun ContinueReadingRow(
    item: LibraryItem,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val geometryModifier = Modifier.documentHero(documentHeroKey(item.uri))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(geometryModifier)
            .pressScaleEffect(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppSpacing.objectRowHeight)
                .padding(horizontal = AppSpacing.screenPadding, vertical = AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            Icon(
                imageVector = getDocumentFormatIcon(item.extension),
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatMiddleTruncated(item.name, 40),
                    style = typography.bodyLargeStrong,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = itemMeta(item),
                    style = typography.caption.withTabularFigures(),
                    color = colors.inkMuted,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        ProgressGauge(
            fraction = item.progressFraction,
            modifier = Modifier.padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.small
            )
        )

        Spacer(modifier = Modifier.height(AppSpacing.small))
        EditorialRule(
            modifier = Modifier.padding(horizontal = AppSpacing.screenPadding),
            thickness = AppSpacing.hairline,
            color = colors.line
        )
    }
}

@Composable
private fun ProgressGauge(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    if (fraction <= 0f) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppSpacing.rule)
            .background(colors.track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(colors.accent)
        )
    }
}

private fun itemMeta(item: LibraryItem): String {
    val extension = item.extension.uppercase()
    val percent = (item.progressFraction * 100).toInt()
    return if (percent > 0) {
        "$extension · ${item.formattedSize} · $percent%"
    } else {
        "$extension · ${item.formattedSize}"
    }
}

/**
 * Returns authentic document format icon for the given extension.
 */
private fun getDocumentFormatIcon(extension: String): ImageVector {
    return when (extension.lowercase()) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "doc", "docx", "dot", "dotx", "odt", "rtf" -> Icons.Outlined.Description
        "xls", "xlsx", "xlsm", "csv", "ods" -> Icons.Outlined.TableChart
        "ppt", "pptx", "pps", "odp" -> Icons.Outlined.Slideshow
        "txt", "log", "md", "json", "xml" -> Icons.AutoMirrored.Outlined.Article
        "jpg", "jpeg", "png", "webp", "gif", "svg" -> Icons.Outlined.Image
        "mp4", "mkv", "mov", "avi" -> Icons.Outlined.Videocam
        "mp3", "flac", "wav", "m4a", "ogg" -> Icons.Outlined.AudioFile
        "zip", "rar", "7z", "tar", "gz" -> Icons.Outlined.FolderZip
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}
