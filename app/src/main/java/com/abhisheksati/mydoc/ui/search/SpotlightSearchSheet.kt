package com.abhisheksati.mydoc.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.components.LiquidGlassTokens
import com.abhisheksati.mydoc.designsystem.components.liquidGlass
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.domain.model.LibraryItem

/**
 * Apple Spotlight-style Instant Search Overlay.
 * Searches across all indexed documents, photos, and media with live query filtering,
 * format category chips, and tactile liquid glass cards.
 */
@Composable
fun SpotlightSearchSheet(
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val colors = AppTheme.colors
        val typography = AppTheme.typography
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        var query by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        val filteredItems = remember(items, query, selectedFilter) {
            val q = query.trim().lowercase()
            items.filter { item ->
                val matchesQuery = q.isEmpty() ||
                        item.name.lowercase().contains(q) ||
                        item.extension.lowercase().contains(q) ||
                        (item.folder != null && item.folder.lowercase().contains(q))

                val matchesFilter = when (selectedFilter) {
                    "PDF" -> item.extension.lowercase() == "pdf"
                    "Word" -> item.extension.lowercase() in setOf("doc", "docx", "odt", "rtf")
                    "Excel" -> item.extension.lowercase() in setOf("xls", "xlsx", "csv", "ods")
                    "PPT" -> item.extension.lowercase() in setOf("ppt", "pptx", "odp")
                    "Media" -> item.extension.lowercase() in setOf("jpg", "jpeg", "png", "mp4", "mkv", "mp3", "flac")
                    else -> true
                }

                matchesQuery && matchesFilter
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xF20E1015) else Color(0xF2F8F9FB))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.ink
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Liquid Glass Input Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .liquidGlass(
                                isDarkTheme = isDarkTheme,
                                shape = RoundedCornerShape(24.dp),
                                elevation = 2.dp
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search documents, notes, media...",
                                        style = typography.body.copy(fontSize = 14.5.sp),
                                        color = colors.inkMuted
                                    )
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    textStyle = typography.body.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.ink
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(colors.accent)
                                )
                            }

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = colors.inkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Pills
                val filterPills = listOf("All", "PDF", "Word", "Excel", "PPT", "Media")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterPills.forEach { label ->
                        val isSelected = selectedFilter == label
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) colors.accent.copy(alpha = 0.18f) else (if (isDarkTheme) Color(0x33252836) else Color(0x1F000000)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedFilter = label }
                        ) {
                            Text(
                                text = label,
                                style = typography.caption.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) colors.accent else colors.inkMuted,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search Results or Empty State
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.inkMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (query.isEmpty()) "Type to search files" else "No matching files found",
                                style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (query.isEmpty()) "Search by document title, format, or folder" else "Try checking for typos or searching a different term",
                                style = typography.caption.copy(fontSize = 13.sp),
                                color = colors.inkMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(
                            items = filteredItems,
                            key = { it.uri }
                        ) { item ->
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleEffect(onClick = {
                                        onDismiss()
                                        onItemClick(item)
                                    })
                                    .liquidGlass(
                                        isDarkTheme = isDarkTheme,
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = 2.dp
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(itemGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                            color = colors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.extension.uppercase(),
                                                style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                color = colors.accent
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "•", color = colors.inkMuted, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.formattedSize,
                                                style = typography.caption.copy(fontSize = 11.sp),
                                                color = colors.inkMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
