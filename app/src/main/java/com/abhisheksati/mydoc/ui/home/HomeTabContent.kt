package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.animation.documentHero
import com.abhisheksati.mydoc.designsystem.animation.documentHeroKey
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.components.DocumentFilterChips
import com.abhisheksati.mydoc.designsystem.components.formatMiddleTruncated
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.DocumentFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.tools.model.PdfTool

/**
 * Editorial Document Studio Home Dashboard:
 * 1. Confident typography: Split wordmark `My` (light) + `Doc` (ExtraBold Ink-Violet).
 * 2. High-density Quick Actions strip (Scan, Merge, Compress, Sign, Img to PDF).
 * 3. Editorial "Jump Back In / Now Reading" card with authentic document badging.
 * 4. Inset-grouped document feed with 0.5dp hairline borders and middle-truncated filenames.
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
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    var showClearRecentDialog by remember { mutableStateOf(false) }
    var selectedDocumentFilter by remember { mutableStateOf(DocumentFilter.ALL) }

    val filteredRecentItems = remember(recentItems, selectedDocumentFilter) {
        recentItems.filter { selectedDocumentFilter.matches(it.extension) }
    }

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
        // 1. Persistent Editorial Top Bar
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
            // 2. High-Density Quick Actions Strip
            item(key = "quick_actions_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary Action: Scan Document
                    Surface(
                        shape = AppRadius.pillShape,
                        color = colors.accent,
                        modifier = Modifier.pressScaleEffect(onClick = onScanClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Scan Document",
                                style = typography.bodyStrong,
                                color = colors.onAccent
                            )
                        }
                    }

                    // Secondary Editorial Action Pills
                    EditorialActionPill(
                        title = "Merge",
                        icon = Icons.AutoMirrored.Outlined.MergeType,
                        onClick = { onToolClick(PdfTool.MERGE) }
                    )

                    EditorialActionPill(
                        title = "Compress",
                        icon = Icons.Outlined.Compress,
                        onClick = { onToolClick(PdfTool.COMPRESS) }
                    )

                    EditorialActionPill(
                        title = "Sign",
                        icon = Icons.Outlined.Draw,
                        onClick = { onToolClick(PdfTool.SIGN_PDF) }
                    )

                    EditorialActionPill(
                        title = "Img to PDF",
                        icon = Icons.Outlined.PictureAsPdf,
                        onClick = { onToolClick(PdfTool.IMAGE_TO_PDF) }
                    )
                }
            }

            // 3. Editorial "Jump Back In / Now Reading" Card
            val continueItem = continueItems.firstOrNull()
            if (continueItem != null) {
                item(key = "resume_reading_card") {
                    Surface(
                        shape = AppRadius.cardShape,
                        color = colors.surface,
                        border = BorderStroke(0.5.dp, colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .documentHero(documentHeroKey(continueItem.uri))
                            .pressScaleEffect(onClick = { onItemClick(continueItem) })
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tint = DocumentFormatColors.getTint(continueItem.extension)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(AppRadius.microShape)
                                    .background(tint.copy(alpha = if (isDarkTheme) 0.16f else 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getDocumentFormatIcon(continueItem.extension),
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "NOW READING",
                                        style = typography.overline,
                                        color = colors.accent
                                    )
                                    if (continueItem.progressFraction > 0f) {
                                        val pct = (continueItem.progressFraction * 100).toInt()
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• $pct%",
                                            style = typography.caption.withTabularFigures(),
                                            color = colors.inkMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatMiddleTruncated(continueItem.name, 34),
                                    style = typography.bodyStrong,
                                    color = colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (continueItem.progressFraction > 0f) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    LinearProgressIndicator(
                                        progress = { continueItem.progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                            .clip(RoundedCornerShape(1.5.dp)),
                                        color = colors.accent,
                                        trackColor = colors.surfaceMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // 4. Recents Header
            item(key = "recents_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent documents",
                        style = typography.heading,
                        color = colors.ink
                    )
                    if (recentItems.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearRecentDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Clear",
                                style = typography.captionStrong.copy(color = colors.accent)
                            )
                        }
                    }
                }
            }

            // 5. Document Format Filters
            item(key = "filter_chips") {
                DocumentFilterChips(
                    selected = selectedDocumentFilter,
                    onSelected = { selectedDocumentFilter = it },
                    modifier = Modifier.padding(horizontal = AppSpacing.medium)
                )
                Spacer(modifier = Modifier.height(AppSpacing.small))
            }

            // 6. Content-First: Inset-Grouped List
            if (filteredRecentItems.isEmpty()) {
                item(key = "empty_recents") {
                    Surface(
                        shape = AppRadius.cardShape,
                        color = colors.surface,
                        border = BorderStroke(0.5.dp, colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = colors.inkMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (selectedDocumentFilter == DocumentFilter.ALL) "No recent documents" else "No ${selectedDocumentFilter.label} files found",
                                style = typography.bodyStrong,
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (selectedDocumentFilter == DocumentFilter.ALL) "Documents you open or scan will appear here" else "Open a ${selectedDocumentFilter.label} file to view it here",
                                style = typography.caption,
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            } else {
                item(key = "recent_items_group") {
                    Surface(
                        shape = AppRadius.cardShape,
                        color = colors.surface,
                        border = BorderStroke(0.5.dp, colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            filteredRecentItems.forEachIndexed { index, item ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 58.dp),
                                        thickness = 0.5.dp,
                                        color = colors.line
                                    )
                                }
                                RecentItemRow(
                                    item = item,
                                    isDarkTheme = isDarkTheme,
                                    isHero = item.kind == MediaKind.DOCUMENT && item.uri != continueItem?.uri,
                                    onClick = { onItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Editorial Action Pill for compact action strip.
 */
@Composable
private fun EditorialActionPill(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Surface(
        shape = AppRadius.pillShape,
        color = colors.surfaceContainer,
        border = BorderStroke(AppSpacing.hairline, colors.line),
        modifier = Modifier.pressScaleEffect(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = typography.captionStrong,
                color = colors.ink
            )
        }
    }
}

/**
 * Clean Inset-Grouped Document Row:
 * Uses authentic format color, tabular figures, and middle-truncated titles.
 */
@Composable
private fun RecentItemRow(
    item: LibraryItem,
    isDarkTheme: Boolean,
    isHero: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val formatTint = DocumentFormatColors.getTint(item.extension)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isHero) Modifier.documentHero(documentHeroKey(item.uri)) else Modifier)
            .pressScaleEffect(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Authentic format badge (crisp solid tint with subtle surface tint)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(AppRadius.microShape)
                .background(formatTint.copy(alpha = if (isDarkTheme) 0.16f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getDocumentFormatIcon(item.extension),
                contentDescription = null,
                tint = formatTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatMiddleTruncated(item.name, 36),
                style = typography.bodyStrong,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.extension.uppercase(),
                    style = typography.micro,
                    color = formatTint
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "•",
                    style = typography.micro,
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = item.formattedSize,
                    style = typography.micro.withTabularFigures(),
                    color = colors.inkMuted
                )
            }

            if (item.progressFraction > 0f) {
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = colors.accent,
                    trackColor = colors.surfaceMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.inkMuted.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
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
