package com.abhisheksati.mydoc.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.abhisheksati.mydoc.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.kindColor
import com.abhisheksati.mydoc.designsystem.tokens.kindContainer
import com.abhisheksati.mydoc.designsystem.tokens.onKindColor
import com.abhisheksati.mydoc.designsystem.tokens.onKindContainer
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind

@Composable
fun HomeTabContent(
    continueItems: List<LibraryItem>,
    recentItems: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onSearchClick: () -> Unit,
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

    val topItem = continueItems.firstOrNull()
    val nextThreeItems = remember(continueItems) { continueItems.drop(1).take(3) }

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
        // Standard symmetrical 56dp App Header
        AppTabHeader(
            title = "stacks",
            titleContent = {
                Text(
                    text = "stacks",
                    style = typography.headline.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
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

        // 2. Section: Continue
        item(key = "continue_header") {
            Text(
                text = "Continue",
                style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = colors.ink,
                modifier = Modifier.padding(
                    start = AppSpacing.paddingLarge,
                    end = AppSpacing.paddingLarge,
                    top = AppSpacing.paddingSmall,
                    bottom = AppSpacing.paddingMedium
                )
            )
        }

        // 3. Hero Card (28dp radius, kind colour)
        item(key = "hero_card") {
            if (topItem != null) {
                val cardColor = kindColor(topItem.kind, isDarkTheme)
                val textColor = onKindColor(topItem.kind, isDarkTheme)

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = cardColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.paddingLarge)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            haptics.tap()
                            onItemClick(topItem)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Kind badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = textColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getKindIcon(topItem.kind),
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = topItem.kind.name,
                                        style = typography.caption.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = textColor
                                    )
                                }
                            }

                            // Circular Resume Button
                            Surface(
                                shape = CircleShape,
                                color = textColor,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptics.heavy()
                                        onItemClick(topItem)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (topItem.kind == MediaKind.VIDEO || topItem.kind == MediaKind.AUDIO) {
                                            Icons.Filled.PlayArrow
                                        } else {
                                            Icons.AutoMirrored.Filled.ArrowForward
                                        },
                                        contentDescription = "Resume",
                                        tint = cardColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Title
                        Text(
                            text = topItem.name,
                            style = typography.headline.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar & Info
                        if (topItem.progressFraction > 0f) {
                            LinearProgressIndicator(
                                progress = { topItem.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = textColor,
                                trackColor = textColor.copy(alpha = 0.25f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = topItem.formattedProgress.ifEmpty { topItem.formattedSize },
                                style = typography.caption.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = textColor.copy(alpha = 0.9f)
                            )
                            if (topItem.folder != null) {
                                Text(
                                    text = topItem.folder,
                                    style = typography.caption,
                                    color = textColor.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty Hero
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfaceMuted,
                    border = BorderStroke(1.dp, colors.line),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.paddingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome to Stacks",
                            style = typography.title.copy(fontWeight = FontWeight.Bold),
                            color = colors.ink
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your unified offline media engine. Open any document, photo, video, or audio to continue reading or watching here.",
                            style = typography.body.copy(fontSize = 13.sp),
                            color = colors.inkMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4. Next Items (Row of three 22dp tiles)
        if (nextThreeItems.isNotEmpty()) {
            item(key = "next_items_row") {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AppSpacing.paddingLarge),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(nextThreeItems, key = { it.uri }) { item ->
                        val itemColor = kindColor(item.kind, isDarkTheme)
                        val textColor = onKindColor(item.kind, isDarkTheme)

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = itemColor,
                            modifier = Modifier
                                .width(150.dp)
                                .height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    haptics.tap()
                                    onItemClick(item)
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getKindIcon(item.kind),
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = item.typeBadge,
                                        style = typography.caption.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = textColor.copy(alpha = 0.85f)
                                    )
                                }

                                Text(
                                    text = item.name,
                                    style = typography.caption.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = textColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Column {
                                    if (item.progressFraction > 0f) {
                                        LinearProgressIndicator(
                                            progress = { item.progressFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = textColor,
                                            trackColor = textColor.copy(alpha = 0.25f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Text(
                                        text = item.formattedProgress.ifEmpty { item.formattedSize },
                                        style = typography.caption.copy(
                                            fontSize = 10.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = textColor.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Section: Recent with kind-tinted extension squares
        item(key = "recent_header") {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent",
                    style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
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
        }

        if (recentItems.isEmpty()) {
            item(key = "empty_recents") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.paddingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (continueItems.isNotEmpty()) "No other recent items" else "No recent items found",
                        style = typography.caption,
                        color = colors.inkMuted
                    )
                }
            }
        } else {
            items(recentItems, key = { "recent_${it.uri}" }) { item ->
                RecentItemRow(
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


@Composable
private fun RecentItemRow(
    item: LibraryItem,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val containerColor = kindContainer(item.kind, isDarkTheme)
    val badgeTextColor = onKindContainer(item.kind, isDarkTheme)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.paddingLarge, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kind-tinted extension square
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = item.typeBadge,
                    style = typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = badgeTextColor
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
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
                    style = typography.caption.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
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
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = badgeTextColor
                    )
                }
            }
        }
    }
}

fun getKindIcon(kind: MediaKind): ImageVector {
    return when (kind) {
        MediaKind.DOCUMENT -> Icons.AutoMirrored.Outlined.InsertDriveFile
        MediaKind.PHOTO -> Icons.Outlined.Image
        MediaKind.VIDEO -> Icons.Outlined.Videocam
        MediaKind.AUDIO -> Icons.Outlined.Audiotrack
    }
}
