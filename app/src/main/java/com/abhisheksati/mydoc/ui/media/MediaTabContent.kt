package com.abhisheksati.mydoc.ui.media

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.MediaFilter
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.viewmodel.MediaViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.sp

@Composable
fun MediaTabContent(
    viewModel: MediaViewModel,
    onOpenMedia: (MediaItem, List<MediaItem>) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    onOpenTrash: (() -> Unit)? = null,
    trashCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        viewModel.setPermissionGranted(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermission(context)
    }

    val requestPermissions = {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val displayedGroups = remember(uiState.groups, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.groups
        } else {
            uiState.groups.mapNotNull { group ->
                val matched = group.items.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
                if (matched.isNotEmpty()) group.copy(items = matched) else null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AppTabHeader(
            title = "Media",
            onSearchClick = { isSearchActive = !isSearchActive },
            isSearchActive = isSearchActive || searchQuery.isNotEmpty(),
            searchContent = {
                AppSearchField(
                    query = searchQuery,
                    onQueryChanged = { searchQuery = it },
                    placeholder = "Search photos & videos..."
                )
            },
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onSettingsClick = onSettingsClick,
            moreMenuContent = { onDismiss ->
                DropdownMenuItem(
                    text = { Text("Refresh media", style = typography.body) },
                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null, tint = colors.accent) },
                    onClick = {
                        onDismiss()
                        viewModel.checkPermission(context)
                    }
                )
                if (onOpenTrash != null) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Recycle Bin", style = typography.body)
                                if (trashCount > 0) {
                                    Badge { Text(trashCount.toString()) }
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = colors.accent)
                        },
                        onClick = {
                            onDismiss()
                            onOpenTrash()
                        }
                    )
                }
                if (onSettingsClick != null) {
                    DropdownMenuItem(
                        text = { Text("Settings", style = typography.body) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Settings, contentDescription = null, tint = colors.textSecondary)
                        },
                        onClick = {
                            onDismiss()
                            onSettingsClick()
                        }
                    )
                }
            },
            bottomContent = if (uiState.hasPermission) {
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFilterChip(
                            selected = uiState.filter == MediaFilter.ALL,
                            onClick = { viewModel.setFilter(MediaFilter.ALL) },
                            label = "All"
                        )
                        AppFilterChip(
                            selected = uiState.filter == MediaFilter.PHOTOS,
                            onClick = { viewModel.setFilter(MediaFilter.PHOTOS) },
                            label = "Photos"
                        )
                        AppFilterChip(
                            selected = uiState.filter == MediaFilter.VIDEOS,
                            onClick = { viewModel.setFilter(MediaFilter.VIDEOS) },
                            label = "Videos"
                        )
                    }
                }
            } else null
        )

        if (!uiState.hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.paddingLarge),
                contentAlignment = Alignment.Center
            ) {
                AppEmptyState(
                    title = "Photos & Videos",
                    message = "Allow access to your device photos and videos to view them offline in mydoc.",
                    actionLabel = "Allow access",
                    onAction = requestPermissions
                )
            }
        } else {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppProgressIndicator()
                }
            } else if (displayedGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.paddingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    AppEmptyState(
                        title = if (searchQuery.isNotEmpty()) "No matching media" else "No media found",
                        message = if (searchQuery.isNotEmpty()) "No photos or videos matched \"$searchQuery\"." else "No photos or videos were found on your device."
                    )
                }
            } else {
                // Media grid grouped by date
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        end = 2.dp,
                        top = 2.dp,
                        bottom = 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    displayedGroups.forEach { group ->
                        item(span = { GridItemSpan(3) }) {
                            AppSectionHeader(
                                title = group.title,
                                modifier = Modifier.padding(
                                    start = AppSpacing.paddingLarge,
                                    top = AppSpacing.paddingMedium,
                                    bottom = AppSpacing.paddingSmall
                                )
                            )
                        }

                        items(
                            items = group.items,
                            key = { it.uri.toString() }
                        ) { item ->
                            MediaThumbnailItem(
                                item = item,
                                onClick = {
                                    val relevantList = if (item.isVideo) {
                                        uiState.items.filter { it.isVideo }
                                    } else {
                                        uiState.items.filter { !it.isVideo }
                                    }
                                    onOpenMedia(item, relevantList)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnailItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val typography = LocalAppTypography.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFF1E1E20))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video duration pill on bottom-right
        if (item.isVideo && item.formattedDuration.isNotEmpty()) {
            Surface(
                color = Color(0xB3000000),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = item.formattedDuration,
                        style = typography.caption.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
