package com.abhisheksati.mydoc.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.automirrored.outlined.MergeType
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
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.LiquidGlassTokens
import com.abhisheksati.mydoc.designsystem.components.liquidGlass

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
 * Focused on the essentials: Camera Scan, Category Quick Links, and Filtered Recent Files.
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

            // 3. Primary Action: Scan Document (Apple Liquid Glass Hero Card)
            item(key = "hero_scan_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .pressScaleEffect(onClick = onScanClick)
                        .liquidGlass(
                            isDarkTheme = isDarkTheme,
                            tintColor = Color(0xFF007AFF),
                            shape = RoundedCornerShape(24.dp),
                            elevation = 6.dp
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Document",
                                style = typography.title.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Create a crisp PDF with your camera",
                                style = typography.body.copy(fontSize = 13.sp),
                                color = colors.inkMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(LiquidGlassTokens.AzureLiquid),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = "Scan Now",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. Jump Back In (Continue Reading Hero Card)
            val continueItem = continueItems.firstOrNull()
            if (continueItem != null) {
                item(key = "continue_reading_hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .pressScaleEffect(onClick = { onItemClick(continueItem) })
                            .liquidGlass(
                                isDarkTheme = isDarkTheme,
                                tintColor = Color(0xFFAF52DE),
                                shape = RoundedCornerShape(22.dp),
                                elevation = 5.dp
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(LiquidGlassTokens.VioletLiquid),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoStories,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Jump Back In",
                                        style = typography.caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                                        color = colors.accent
                                    )
                                }

                                if (continueItem.progressFraction > 0f) {
                                    val pct = (continueItem.progressFraction * 100).toInt()
                                    Text(
                                        text = "$pct% completed",
                                        style = typography.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                        color = colors.inkMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = continueItem.name,
                                        style = typography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                        color = colors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${continueItem.extension.uppercase()} • ${continueItem.formattedSize}",
                                        style = typography.caption.copy(fontSize = 12.sp),
                                        color = colors.inkMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.accent,
                                    modifier = Modifier.clickable { onItemClick(continueItem) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Resume",
                                            style = typography.caption.copy(
                                                color = colors.onAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = null,
                                            tint = colors.onAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            if (continueItem.progressFraction > 0f) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { continueItem.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = colors.accent,
                                    trackColor = colors.surfaceMuted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // 5. Quick Tools Strip (1-tap shortcuts)
            item(key = "quick_tools_section") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Quick Tools",
                        style = typography.heading.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickToolPill(
                            title = "Combine PDF",
                            icon = Icons.AutoMirrored.Outlined.MergeType,
                            gradient = LiquidGlassTokens.AzureLiquid,
                            isDarkTheme = isDarkTheme,
                            onClick = { onToolClick(PdfTool.MERGE) }
                        )
                        QuickToolPill(
                            title = "Compress",
                            icon = Icons.Outlined.Compress,
                            gradient = LiquidGlassTokens.AuroraLiquid,
                            isDarkTheme = isDarkTheme,
                            onClick = { onToolClick(PdfTool.COMPRESS) }
                        )
                        QuickToolPill(
                            title = "Sign Doc",
                            icon = Icons.Outlined.Draw,
                            gradient = LiquidGlassTokens.SunsetLiquid,
                            isDarkTheme = isDarkTheme,
                            onClick = { onToolClick(PdfTool.SIGN_PDF) }
                        )
                        QuickToolPill(
                            title = "Img to PDF",
                            icon = Icons.Outlined.PictureAsPdf,
                            gradient = LiquidGlassTokens.AmberLiquid,
                            isDarkTheme = isDarkTheme,
                            onClick = { onToolClick(PdfTool.IMAGE_TO_PDF) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Section: Recent Documents Header
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
            }

            // 6. Sub-Category Filter Chips (Smooth Animated Morph)
            item(key = "document_filter_chips") {
                val docFilters = listOf("All", "PDF", "Word", "Excel", "PPT", "Text")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    docFilters.forEach { label ->
                        val isSelected = selectedDocumentFilter == label
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) colors.accent.copy(alpha = 0.16f) else colors.surfaceContainer,
                            animationSpec = tween(durationMillis = 200),
                            label = "filterChipBg"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (isSelected) colors.accent else colors.inkMuted,
                            animationSpec = tween(durationMillis = 200),
                            label = "filterChipText"
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = chipBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .pressScaleEffect(onClick = { selectedDocumentFilter = label })
                        ) {
                            Text(
                                text = label,
                                style = typography.caption.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = chipText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 7. Recent Items List with Fluid List Animations
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = colors.inkMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
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
                items(
                    items = filteredRecentItems,
                    key = { "recent_${it.uri}" }
                ) { item ->
                    RecentItemRow(
                        item = item,
                        isDarkTheme = isDarkTheme,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.animateItem()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

/**
 * Tactile Expressive Category Tile.
 */
@Composable
private fun CategoryTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tintColor: Color,
    gradient: androidx.compose.ui.graphics.Brush,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Box(
        modifier = modifier
            .pressScaleEffect(onClick = onClick)
            .liquidGlass(
                isDarkTheme = isDarkTheme,
                tintColor = tintColor,
                shape = RoundedCornerShape(20.dp),
                elevation = 4.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Text(
                text = title,
                style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = colors.ink
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = typography.caption.copy(fontSize = 12.sp),
                color = colors.inkMuted
            )
        }
    }
}

/**
 * Clean Recent Document Row with tactile bounce.
 */
@Composable
private fun RecentItemRow(
    item: LibraryItem,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val itemGradient = when (item.extension.lowercase()) {
        "pdf" -> LiquidGlassTokens.AzureLiquid
        "docx", "doc" -> LiquidGlassTokens.IndigoLiquid
        "xlsx", "xls", "csv" -> LiquidGlassTokens.AuroraLiquid
        "pptx", "ppt" -> LiquidGlassTokens.AmberLiquid
        "mp4", "mkv", "mov" -> LiquidGlassTokens.SunsetLiquid
        "mp3", "flac", "wav" -> LiquidGlassTokens.VioletLiquid
        else -> LiquidGlassTokens.AzureLiquid
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .pressScaleEffect(onClick = onClick)
            .liquidGlass(
                isDarkTheme = isDarkTheme,
                shape = RoundedCornerShape(18.dp),
                elevation = 3.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colorful liquid glass icon container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(itemGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.extension.uppercase(),
                        style = typography.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        style = typography.caption.copy(fontSize = 11.sp),
                        color = colors.inkMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.formattedSize,
                        style = typography.caption.copy(fontSize = 11.5.sp),
                        color = colors.inkMuted
                    )
                }

                if (item.progressFraction > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colors.accent,
                        trackColor = colors.surfaceMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.inkMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


/**
 * 1-Tap Quick Tool Shortcut Pill
 */
@Composable
private fun QuickToolPill(
    title: String,
    icon: ImageVector,
    gradient: androidx.compose.ui.graphics.Brush,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Box(
        modifier = Modifier
            .pressScaleEffect(onClick = onClick)
            .liquidGlass(
                isDarkTheme = isDarkTheme,
                shape = RoundedCornerShape(16.dp),
                elevation = 3.dp
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = title,
                style = typography.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = colors.ink
            )
        }
    }
}
