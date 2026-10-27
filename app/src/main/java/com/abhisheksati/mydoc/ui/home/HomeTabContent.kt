package com.abhisheksati.mydoc.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.Article
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.InterFontFamily
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.tools.model.PdfTool

/**
 * Swiss & Apple Precision Home Dashboard:
 * 1. Persistent Top Chrome (Frosted Glass).
 * 2. High-Density Compact Action Strip (Scan, Merge, Compress, Sign, Img to PDF).
 * 3. Content-First: Recents visible in top 20% of the display.
 * 4. Apple Inset-Grouped list with hairline dividers, zero candy gradients, and authentic document badges.
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
    var selectedDocumentFilter by remember { mutableStateOf("All") }

    val filteredRecentItems = remember(recentItems, selectedDocumentFilter) {
        when (selectedDocumentFilter) {
            "PDF" -> recentItems.filter { it.extension == "pdf" }
            "Word" -> recentItems.filter { it.extension in setOf("doc", "docx", "dot", "dotx", "odt", "rtf") }
            "Excel" -> recentItems.filter { it.extension in setOf("xls", "xlsx", "xlsm", "csv", "ods") }
            "PPT" -> recentItems.filter { it.extension in setOf("ppt", "pptx", "pps", "odp") }
            "Text" -> recentItems.filter { it.extension in setOf("txt", "log", "md", "json", "xml") }
            else -> recentItems
        }
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
        // 1. Persistent Top Bar
        AppTabHeader(
            title = "MyDoc",
            titleContent = {
                Text(
                    text = "MyDoc",
                    fontFamily = InterFontFamily,
                    style = typography.title.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = colors.ink
                )
            },
            onSearchClick = onSearchClick,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onSettingsClick = onSettingsClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp)
        ) {
            // 2. High-Density Quick Actions Strip (48dp height)
            item(key = "quick_actions_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary Action: Scan Document
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.accent,
                        modifier = Modifier.clickable { onScanClick() }
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
                                style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = colors.onAccent
                            )
                        }
                    }

                    // Secondary Precision Action Pills
                    PrecisionActionPill(
                        title = "Merge",
                        icon = Icons.AutoMirrored.Outlined.MergeType,
                        onClick = { onToolClick(PdfTool.MERGE) }
                    )

                    PrecisionActionPill(
                        title = "Compress",
                        icon = Icons.Outlined.Compress,
                        onClick = { onToolClick(PdfTool.COMPRESS) }
                    )

                    PrecisionActionPill(
                        title = "Sign",
                        icon = Icons.Outlined.Draw,
                        onClick = { onToolClick(PdfTool.SIGN_PDF) }
                    )

                    PrecisionActionPill(
                        title = "Img to PDF",
                        icon = Icons.Outlined.PictureAsPdf,
                        onClick = { onToolClick(PdfTool.IMAGE_TO_PDF) }
                    )
                }
            }

            // 3. Compact "Resume Reading" Inset Row (if user has active reading session)
            val continueItem = continueItems.firstOrNull()
            if (continueItem != null) {
                item(key = "resume_reading_card") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surface,
                        border = BorderStroke(0.5.dp, colors.divider.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onItemClick(continueItem) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tint = DocumentFormatColors.getTint(continueItem.extension)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tint.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getDocumentFormatIcon(continueItem.extension),
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Resume Reading",
                                        style = typography.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                        color = colors.accent
                                    )
                                    if (continueItem.progressFraction > 0f) {
                                        val pct = (continueItem.progressFraction * 100).toInt()
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• $pct%",
                                            style = typography.caption.withTabularFigures().copy(fontSize = 11.sp),
                                            color = colors.inkMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = continueItem.name,
                                    style = typography.body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
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
                        text = "Recent Documents",
                        style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )
                    if (recentItems.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearRecentDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Clear",
                                style = typography.caption.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = colors.accent
                                )
                            )
                        }
                    }
                }
            }

            // 5. Minimal Segmented Filter Chips
            item(key = "filter_chips") {
                val docFilters = listOf("All", "PDF", "Word", "Excel", "PPT", "Text")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    docFilters.forEach { label ->
                        val isSelected = selectedDocumentFilter == label
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) colors.ink else colors.surfaceContainer,
                            animationSpec = tween(durationMillis = 150),
                            label = "filterChipBg"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (isSelected) colors.background else colors.inkMuted,
                            animationSpec = tween(durationMillis = 150),
                            label = "filterChipText"
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = chipBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedDocumentFilter = label }
                        ) {
                            Text(
                                text = label,
                                style = typography.caption.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = chipText,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. Content-First: Apple Inset-Grouped List
            if (filteredRecentItems.isEmpty()) {
                item(key = "empty_recents") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(0.5.dp, colors.divider.copy(alpha = 0.5f)),
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
                                text = if (selectedDocumentFilter == "All") "No recent documents" else "No $selectedDocumentFilter files found",
                                style = typography.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (selectedDocumentFilter == "All") "Documents you open or scan will appear here" else "Open a $selectedDocumentFilter file to view it here",
                                style = typography.caption.copy(fontSize = 12.sp),
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            } else {
                item(key = "recent_items_group") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(0.5.dp, colors.divider.copy(alpha = 0.5f)),
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
                                        color = colors.divider.copy(alpha = 0.35f)
                                    )
                                }
                                RecentItemRow(
                                    item = item,
                                    isDarkTheme = isDarkTheme,
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
 * Precision Action Pill for compact action strip.
 */
@Composable
private fun PrecisionActionPill(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                style = typography.caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                color = colors.ink
            )
        }
    }
}

/**
 * Clean Inset-Grouped Document Row:
 * Uses authentic format color, tabular figures, and zero candy gradients.
 */
@Composable
private fun RecentItemRow(
    item: LibraryItem,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val formatTint = DocumentFormatColors.getTint(item.extension)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Authentic format badge (crisp solid tint with subtle surface tint)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
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
                text = item.name,
                style = typography.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.extension.uppercase(),
                    style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = formatTint
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "•",
                    style = typography.caption.copy(fontSize = 10.sp),
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = item.formattedSize,
                    style = typography.caption.withTabularFigures().copy(fontSize = 11.5.sp),
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
