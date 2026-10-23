package com.abhisheksati.mydoc.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
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
import com.abhisheksati.mydoc.ui.home.getKindIcon
import java.util.Locale

@Composable
fun LibraryTabContent(
    allItems: List<LibraryItem>,
    recentlyAdded: List<LibraryItem>,
    countsByKind: Map<MediaKind, Pair<Int, Long>>,
    onItemClick: (LibraryItem) -> Unit,
    onSettingsClick: () -> Unit,
    onToggleTheme: () -> Unit = {},
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()

    var selectedKind by remember { mutableStateOf<MediaKind?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = selectedKind != null) {
        selectedKind = null
        searchQuery = ""
    }

    AnimatedContent(
        targetState = selectedKind,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "libraryKindTransition",
        modifier = modifier.fillMaxSize()
    ) { currentKind ->
        if (currentKind == null) {
            var isSearchActive by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
            ) {
                AppTabHeader(
                    title = "Library",
                    onSearchClick = {
                        isSearchActive = !isSearchActive
                    },
                    isSearchActive = isSearchActive || searchQuery.isNotBlank(),
                    searchContent = {
                        AppSearchField(
                            query = searchQuery,
                            onQueryChanged = { searchQuery = it },
                            placeholder = "Search documents, photos, video, audio...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onSettingsClick = onSettingsClick
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    // 2x2 Kind Tiles (Documents, Photos, Videos, Audio)
                    if (searchQuery.isBlank()) {
                    item(key = "lib_tiles_grid") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.paddingLarge),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Row 1: Documents | Photos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                KindTile(
                                    kind = MediaKind.DOCUMENT,
                                    label = "Documents",
                                    stat = countsByKind[MediaKind.DOCUMENT] ?: Pair(0, 0L),
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        haptics.tap()
                                        selectedKind = MediaKind.DOCUMENT
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                KindTile(
                                    kind = MediaKind.PHOTO,
                                    label = "Photos",
                                    stat = countsByKind[MediaKind.PHOTO] ?: Pair(0, 0L),
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        haptics.tap()
                                        selectedKind = MediaKind.PHOTO
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Videos | Audio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                KindTile(
                                    kind = MediaKind.VIDEO,
                                    label = "Videos",
                                    stat = countsByKind[MediaKind.VIDEO] ?: Pair(0, 0L),
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        haptics.tap()
                                        selectedKind = MediaKind.VIDEO
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                KindTile(
                                    kind = MediaKind.AUDIO,
                                    label = "Audio",
                                    stat = countsByKind[MediaKind.AUDIO] ?: Pair(0, 0L),
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        haptics.tap()
                                        selectedKind = MediaKind.AUDIO
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Recently Added Section / Search Results
                item(key = "lib_recents_title") {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Recently added" else "Search results",
                        style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink,
                        modifier = Modifier.padding(horizontal = AppSpacing.paddingLarge, vertical = AppSpacing.paddingSmall)
                    )
                }

                val displayedList = if (searchQuery.isNotBlank()) {
                    allItems.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
                } else {
                    recentlyAdded
                }

                if (displayedList.isEmpty()) {
                    item(key = "lib_empty_list") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.paddingLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No items match \"$searchQuery\"" else "No items added yet",
                                style = typography.caption,
                                color = colors.inkMuted
                            )
                        }
                    }
                } else {
                    items(displayedList, key = { "lib_item_${it.uri}" }) { item ->
                        LibraryItemRow(
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
    } else {
            // Kind Drill-down View
            KindDrillDownView(
                kind = currentKind,
                items = allItems.filter { it.kind == currentKind },
                isDarkTheme = isDarkTheme,
                onBack = { selectedKind = null },
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun KindTile(
    kind: MediaKind,
    label: String,
    stat: Pair<Int, Long>,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = AppTheme.typography
    val tileColor = kindColor(kind, isDarkTheme)
    val textColor = onKindColor(kind, isDarkTheme)

    val count = stat.first
    val sizeBytes = stat.second

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = tileColor,
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getKindIcon(kind),
                    contentDescription = label,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "$count",
                    style = typography.title.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = textColor
                )
            }

            Column {
                Text(
                    text = label,
                    style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = textColor
                )
                Text(
                    text = formatByteSize(sizeBytes),
                    style = typography.caption.copy(
                        fontSize = 11.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun KindDrillDownView(
    kind: MediaKind,
    items: List<LibraryItem>,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onItemClick: (LibraryItem) -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val haptics = rememberAppHaptics()
    var query by remember { mutableStateOf("") }

    val filtered = remember(items, query) {
        if (query.isBlank()) items
        else items.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.paddingSmall, vertical = AppSpacing.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptics.tap()
                    onBack()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.ink
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (kind) {
                        MediaKind.DOCUMENT -> "Documents"
                        MediaKind.PHOTO -> "Photos"
                        MediaKind.VIDEO -> "Videos"
                        MediaKind.AUDIO -> "Audio"
                    },
                    style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink
                )
                Text(
                    text = "${filtered.size} items",
                    style = typography.caption.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    color = colors.inkMuted
                )
            }
        }

        // Sub-view presentation
        when (kind) {
            MediaKind.PHOTO -> {
                // 3-column square grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.uri }) { photo ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceMuted,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    haptics.tap()
                                    onItemClick(photo)
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photo.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            MediaKind.VIDEO -> {
                // Video list with 16:9 thumbnail and duration badge
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filtered, key = { it.uri }) { video ->
                        VideoItemRow(
                            item = video,
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onItemClick(video)
                            }
                        )
                    }
                }
            }
            else -> {
                // Documents & Audio list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(filtered, key = { it.uri }) { docOrAudio ->
                        LibraryItemRow(
                            item = docOrAudio,
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                haptics.tap()
                                onItemClick(docOrAudio)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoItemRow(
    item: LibraryItem,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.paddingLarge, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 16:9 Video Thumbnail
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.surfaceMuted,
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.uri)
                        .videoFrameMillis(2000L)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay
                Surface(
                    shape = CircleShape,
                    color = Color(0x88000000),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.formattedSize,
                    style = typography.caption.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    color = colors.inkMuted
                )
                if (item.formattedProgress.isNotEmpty()) {
                    Text(text = "•", style = typography.caption.copy(fontSize = 12.sp), color = colors.inkMuted)
                    Text(
                        text = item.formattedProgress,
                        style = typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        color = kindColor(MediaKind.VIDEO, isDarkTheme)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryItemRow(
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
            }
        }
    }
}

private fun formatByteSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
