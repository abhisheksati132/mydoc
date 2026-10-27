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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.BricolageFontFamily
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.tools.model.PdfTool
import java.util.Calendar

/**
 * Material 3 Expressive Home Dashboard for mydoc.
 *
 * Designed for everyday human users:
 * 1. Warm, time-of-day conversational greeting with expressive typography.
 * 2. Instant Search pill.
 * 3. Primary Hero Action: One-tap Camera Document Scanner (28dp squircle).
 * 4. 4 High-touch Expressive Category Cards: Documents, Photos, Videos, Audio.
 * 5. Everyday Popular Tools: Merge, Compress, Sign, Reorder, Protect.
 * 6. "Jump Back In" Recent Documents with clear document format badges.
 */
@Composable
fun HomeTabContent(
    continueItems: List<LibraryItem>,
    recentItems: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onToolClick: (PdfTool) -> Unit,
    onSettingsClick: () -> Unit,
    onToggleTheme: () -> Unit = {},
    onClearRecent: () -> Unit = {},
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    var showClearRecentDialog by remember { mutableStateOf(false) }

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
                haptics.tap()
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
        // 1. Material 3 Expressive Header
        AppTabHeader(
            title = "mydoc",
            titleContent = {
                Text(
                    text = "mydoc",
                    fontFamily = BricolageFontFamily,
                    style = typography.headline.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp
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
            // 2. Greeting & Search Pill
            item(key = "greeting_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = greeting,
                        fontFamily = BricolageFontFamily,
                        style = typography.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "What would you like to open today?",
                        style = typography.body.copy(fontSize = 13.5.sp),
                        color = colors.inkMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expressive Search Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDarkTheme) Color(0xFF18181B) else Color(0xFFF1F1F4),
                        border = BorderStroke(1.dp, colors.line.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                haptics.tap()
                                onSearchClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = colors.inkMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Search documents, photos, notes...",
                                style = typography.body.copy(fontSize = 13.5.sp),
                                color = colors.inkMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Primary Hero Action: Scan Document (28dp Squircle)
            item(key = "hero_scan_card") {
                val scanBgColor = if (isDarkTheme) Color(0xFF1E2235) else Color(0xFFEEF2FF)
                val scanAccent = if (isDarkTheme) Color(0xFF93A4FF) else Color(0xFF3B52F5)
                val scanInk = if (isDarkTheme) Color(0xFFF4F6FF) else Color(0xFF1E293B)

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = scanBgColor,
                    border = BorderStroke(1.dp, scanAccent.copy(alpha = if (isDarkTheme) 0.25f else 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            haptics.heavy()
                            onScanClick()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = scanAccent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "CAMERA SCANNER",
                                    style = typography.caption.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = scanAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Scan Document",
                                fontFamily = BricolageFontFamily,
                                style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                color = scanInk
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "Snap photos of receipts, notes or forms into a PDF",
                                style = typography.caption.copy(fontSize = 12.5.sp),
                                color = scanInk.copy(alpha = 0.75f)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Expressive 54dp Round Action Button
                        Surface(
                            shape = CircleShape,
                            color = scanAccent,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CameraAlt,
                                    contentDescription = "Scan Now",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. Quick Categories (2x2 Expressive Grid)
            item(key = "categories_section") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Categories",
                        fontFamily = BricolageFontFamily,
                        style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: Documents & Photos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        M3CategoryCard(
                            title = "Documents",
                            subtitle = "PDF, Word, Excel",
                            icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                            accentColor = Color(0xFF2563EB),
                            containerColor = if (isDarkTheme) Color(0xFF132038) else Color(0xFFEFF6FF),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onCategoryClick(FileCategory.DOCUMENTS)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        M3CategoryCard(
                            title = "Photos",
                            subtitle = "Images & Gallery",
                            icon = Icons.Outlined.Image,
                            accentColor = Color(0xFFE11D48),
                            containerColor = if (isDarkTheme) Color(0xFF2B1420) else Color(0xFFFFF1F2),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onCategoryClick(FileCategory.IMAGES)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Videos & Audio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        M3CategoryCard(
                            title = "Videos",
                            subtitle = "Clips & Movies",
                            icon = Icons.Outlined.Videocam,
                            accentColor = Color(0xFFD97706),
                            containerColor = if (isDarkTheme) Color(0xFF2C1E10) else Color(0xFFFFFBEB),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onCategoryClick(FileCategory.VIDEOS)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        M3CategoryCard(
                            title = "Audio",
                            subtitle = "Voice & Music",
                            icon = Icons.Outlined.Audiotrack,
                            accentColor = Color(0xFF059669),
                            containerColor = if (isDarkTheme) Color(0xFF0F261C) else Color(0xFFECFDF5),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onCategoryClick(FileCategory.AUDIO)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. Everyday Popular Tools (Expressive Horizontal Chips)
            item(key = "popular_tools_section") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Popular Tools",
                            fontFamily = BricolageFontFamily,
                            style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = colors.ink
                        )
                        Text(
                            text = "Easy to use",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.inkMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        M3ToolPill(
                            label = "Combine PDFs",
                            icon = Icons.AutoMirrored.Outlined.MergeType,
                            color = Color(0xFF3B82F6),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onToolClick(PdfTool.MERGE)
                            }
                        )
                        M3ToolPill(
                            label = "Make Smaller",
                            icon = Icons.Outlined.Compress,
                            color = Color(0xFF10B981),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onToolClick(PdfTool.COMPRESS)
                            }
                        )
                        M3ToolPill(
                            label = "Sign PDF",
                            icon = Icons.Outlined.Draw,
                            color = Color(0xFFF59E0B),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onToolClick(PdfTool.SIGN_PDF)
                            }
                        )
                        M3ToolPill(
                            label = "Pick Pages",
                            icon = Icons.Outlined.GridOn,
                            color = Color(0xFF8B5CF6),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onToolClick(PdfTool.REORDER_PAGES)
                            }
                        )
                        M3ToolPill(
                            label = "Lock PDF",
                            icon = Icons.Outlined.Lock,
                            color = Color(0xFFEF4444),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onToolClick(PdfTool.PROTECT)
                            }
                        )
                        M3ToolPill(
                            label = "Photos to PDF",
                            icon = Icons.Outlined.Image,
                            color = Color(0xFF06B6D4),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onToolClick(PdfTool.IMAGE_TO_PDF)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 6. Section: Recent Documents
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
                        fontFamily = BricolageFontFamily,
                        style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )
                    if (recentItems.isNotEmpty() || continueItems.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                haptics.tap()
                                showClearRecentDialog = true
                            }
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
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (recentItems.isEmpty() && continueItems.isEmpty()) {
                item(key = "empty_recents") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDarkTheme) Color(0xFF18181B) else Color(0xFFF8F8FA),
                        border = BorderStroke(1.dp, colors.line.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accent.copy(alpha = 0.12f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No recent files yet",
                                fontFamily = BricolageFontFamily,
                                style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan a page or tap any category above to get started",
                                style = typography.caption.copy(fontSize = 12.5.sp),
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            } else {
                items(recentItems, key = { "recent_${it.uri}" }) { item ->
                    M3RecentItemCard(
                        item = item,
                        isDarkTheme = isDarkTheme,
                        onClick = {
                            haptics.tap()
                            onItemClick(item)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 24dp Rounded Material 3 Expressive Category Card.
 */
@Composable
private fun M3CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = AppTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.12f)),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = if (isDarkTheme) 0.28f else 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontFamily = BricolageFontFamily,
                style = typography.body.copy(fontSize = 15.5.sp, fontWeight = FontWeight.Bold),
                color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 11.5.sp),
                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}

/**
 * Material 3 Expressive Tool Pill with clean icon and label.
 */
@Composable
private fun M3ToolPill(
    label: String,
    icon: ImageVector,
    color: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val typography = AppTheme.typography

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) Color(0xFF18181B) else Color(0xFFF4F4F7),
        border = BorderStroke(1.dp, color.copy(alpha = if (isDarkTheme) 0.35f else 0.25f)),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.16f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            )
        }
    }
}

/**
 * Material 3 Expressive Recent Item Card.
 */
@Composable
private fun M3RecentItemCard(
    item: LibraryItem,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val (badgeBg, badgeText) = when (item.typeBadge) {
        "PDF" -> (if (isDarkTheme) Color(0xFF351515) else Color(0xFFFEE2E2)) to Color(0xFFEF4444)
        "WORD", "DOC", "DOCX" -> (if (isDarkTheme) Color(0xFF14223A) else Color(0xFFDBEAFE)) to Color(0xFF3B82F6)
        "EXCEL", "XLS", "XLSX", "CSV" -> (if (isDarkTheme) Color(0xFF10281D) else Color(0xFFDCFCE7)) to Color(0xFF10B981)
        "PPT", "PPTX" -> (if (isDarkTheme) Color(0xFF332010) else Color(0xFFFEF3C7)) to Color(0xFFF59E0B)
        "VID", "MP4", "MKV" -> (if (isDarkTheme) Color(0xFF2B1638) else Color(0xFFF3E8FF)) to Color(0xFF8B5CF6)
        "AUD", "MP3", "M4A" -> (if (isDarkTheme) Color(0xFF0E2822) else Color(0xFFCCFBF1)) to Color(0xFF14B8A6)
        "IMG", "JPG", "PNG" -> (if (isDarkTheme) Color(0xFF311525) else Color(0xFFFCE7F3)) to Color(0xFFEC4899)
        else -> (if (isDarkTheme) Color(0xFF202024) else Color(0xFFF1F1F4)) to colors.inkMuted
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkTheme) Color(0xFF151518) else Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, colors.line.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Material 3 Expressive Document Type Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeBg,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.typeBadge,
                        style = typography.caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = badgeText
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // File Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

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

                    if (item.formattedProgress.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.inkMuted
                        )
                        Text(
                            text = item.formattedProgress,
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeText
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.inkMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
