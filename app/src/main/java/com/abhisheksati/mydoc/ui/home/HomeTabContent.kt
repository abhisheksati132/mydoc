package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
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
import com.abhisheksati.mydoc.designsystem.tokens.InterFontFamily
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.tools.model.PdfTool
import java.util.Calendar

/**
 * Clean, calm native Home Dashboard.
 * Focused on the two things users need most: Quick Camera Scan and Recent Files.
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

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Welcome back"
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
        // 1. Native Header
        AppTabHeader(
            title = "MyDoc",
            titleContent = {
                Text(
                    text = "MyDoc",
                    fontFamily = InterFontFamily,
                    style = typography.title.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
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
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 2. Calm Human Greeting
            item(key = "greeting_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = greeting,
                        style = typography.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your documents and files",
                        style = typography.body.copy(fontSize = 13.5.sp),
                        color = colors.inkMuted
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Primary Action: Scan Document
            item(key = "hero_scan_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onScanClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan document",
                                style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Create a PDF using your camera",
                                style = typography.body.copy(fontSize = 13.sp),
                                color = colors.inkMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Surface(
                            shape = CircleShape,
                            color = colors.accent,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CameraAlt,
                                    contentDescription = "Scan Now",
                                    tint = colors.onAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. Quick Categories (Unified Surface Tiles)
            item(key = "categories_section") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Categories",
                        style = typography.heading.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 1: Documents & Photos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryTile(
                            title = "Documents",
                            subtitle = "PDF, Word, Excel",
                            icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                            color = Color(0xFF2563EB),
                            onClick = { onCategoryClick(FileCategory.DOCUMENTS) },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryTile(
                            title = "Photos",
                            subtitle = "Images & Gallery",
                            icon = Icons.Outlined.Image,
                            color = Color(0xFFE11D48),
                            onClick = { onCategoryClick(FileCategory.IMAGES) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Videos & Audio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryTile(
                            title = "Videos",
                            subtitle = "Clips & Movies",
                            icon = Icons.Outlined.Videocam,
                            color = Color(0xFF7C3AED),
                            onClick = { onCategoryClick(FileCategory.VIDEOS) },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryTile(
                            title = "Audio",
                            subtitle = "Recordings & Music",
                            icon = Icons.Outlined.Audiotrack,
                            color = Color(0xFF059669),
                            onClick = { onCategoryClick(FileCategory.AUDIO) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            // 5. Section: Recent Documents
            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent",
                        style = typography.heading.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )
                    if (recentItems.isNotEmpty() || continueItems.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearRecentDialog = true }
                        ) {
                            Text(
                                text = "Clear",
                                style = typography.caption.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = colors.accent
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Sub-category filters for documents
            item(key = "document_filter_chips") {
                val docFilters = listOf(
                    "All" to colors.accent,
                    "PDF" to Color(0xFF2563EB),
                    "Word" to Color(0xFF1D4ED8),
                    "Excel" to Color(0xFF059669),
                    "PPT" to Color(0xFFD97706),
                    "Text" to Color(0xFF0D9488)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    docFilters.forEach { (label, filterColor) ->
                        val isSelected = selectedDocumentFilter == label
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) filterColor.copy(alpha = 0.16f) else colors.surfaceContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedDocumentFilter = label }
                        ) {
                            Text(
                                text = label,
                                style = typography.caption.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) filterColor else colors.inkMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (filteredRecentItems.isEmpty()) {
                item(key = "empty_recents") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accent.copy(alpha = 0.12f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (selectedDocumentFilter == "All") "No recent files" else "No $selectedDocumentFilter files found",
                                style = typography.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (selectedDocumentFilter == "All") "Files you open or scan will appear here" else "Open a $selectedDocumentFilter file to see it here",
                                style = typography.caption.copy(fontSize = 12.sp),
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            } else {
                items(filteredRecentItems, key = { "recent_${it.uri}" }) { item ->
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

/**
 * Clean, consistent category tile with vibrant identity colors.
 */
@Composable
private fun CategoryTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color = Color.Unspecified,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val tileColor = if (color != Color.Unspecified) color else colors.accent

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tileColor.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tileColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = typography.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                color = colors.ink
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 11.5.sp),
                color = colors.inkMuted
            )
        }
    }
}

/**
 * Clean Recent Document Row with format-specific color badge.
 */
@Composable
private fun RecentItemRow(
    item: LibraryItem,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val badgeColor = when (item.typeBadge.uppercase()) {
        "PDF" -> Color(0xFF2563EB)
        "DOC", "WORD", "DOCX" -> Color(0xFF1D4ED8)
        "XLS", "EXCEL", "XLSX", "CSV" -> Color(0xFF059669)
        "PPT", "PPTX" -> Color(0xFFD97706)
        "IMG" -> Color(0xFFE11D48)
        "VID" -> Color(0xFF7C3AED)
        "AUD" -> Color(0xFF0D9488)
        else -> colors.accent
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.typeBadge.take(4),
                        style = typography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.formattedSize,
                        style = typography.caption.copy(fontSize = 12.sp),
                        color = colors.inkMuted
                    )

                    if (item.formattedLastModified.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.inkMuted
                        )
                        Text(
                            text = item.formattedLastModified,
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.inkMuted
                        )
                    }
                }

                if (item.progressFraction > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.5.dp)
                            .clip(CircleShape),
                        color = colors.accent,
                        trackColor = colors.surfaceMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.inkMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
