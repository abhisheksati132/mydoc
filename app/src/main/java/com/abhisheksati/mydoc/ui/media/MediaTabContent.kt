package com.abhisheksati.mydoc.ui.media

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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
import android.media.MediaMetadataRetriever
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.request.videoFrameOption
import coil.request.videoFramePercent
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTabHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
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

private data class MediaNavCategory(
    val filter: MediaFilter,
    val icon: ImageVector,
    val color: Color,
    val label: String
)

@Composable
fun MediaTabContent(
    viewModel: MediaViewModel,
    onOpenMedia: (MediaItem, List<MediaItem>) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onToggleTheme: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    onOpenTrash: (() -> Unit)? = null,
    trashCount: Int = 0,
    onMoveToTrash: ((List<java.io.File>, () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

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
    var selectedUris by remember { mutableStateOf(emptySet<Uri>()) }
    val isSelectionMode = selectedUris.isNotEmpty()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refresh()
            selectedUris = emptySet()
            Toast.makeText(context, "Items deleted", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedUris = emptySet()
    }

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

    val allVisibleItems = remember(displayedGroups) {
        displayedGroups.flatMap { it.items }
    }

    val allVideoItems = remember(uiState.items) {
        uiState.items.filter { it.isVideo }
    }

    val allPhotoItems = remember(uiState.items) {
        uiState.items.filter { !it.isVideo }
    }

    val selectedItems = remember(selectedUris, uiState.items) {
        uiState.items.filter { it.uri in selectedUris }
    }

    val totalSelectedSizeBytes = remember(selectedItems) {
        selectedItems.sumOf { it.sizeBytes }
    }

    val formattedSelectedSize = remember(totalSelectedSizeBytes) {
        if (totalSelectedSizeBytes <= 0) "0 B"
        else {
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(totalSelectedSizeBytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
            String.format(java.util.Locale.US, "%.1f %s", totalSelectedSizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
    }

    fun performDelete(itemsToDelete: List<MediaItem>) {
        val files = itemsToDelete.mapNotNull { item ->
            item.filePath?.let { java.io.File(it).takeIf { f -> f.exists() } }
        }
        if (onMoveToTrash != null && files.isNotEmpty()) {
            onMoveToTrash(files) {
                viewModel.refresh()
            }
            selectedUris = emptySet()
            Toast.makeText(context, "${files.size} items moved to Recycle Bin", Toast.LENGTH_SHORT).show()
            return
        }

        val uris = itemsToDelete.map { it.uri }
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)
                deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            } catch (e: Exception) {
                Toast.makeText(context, "Move to Recycle Bin failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            var recoverable: RecoverableSecurityException? = null
            var count = 0
            for (item in itemsToDelete) {
                try {
                    val rows = context.contentResolver.delete(item.uri, null, null)
                    if (rows > 0) count++
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                        recoverable = e
                        break
                    }
                } catch (_: Exception) {}
            }
            if (recoverable != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteLauncher.launch(IntentSenderRequest.Builder(recoverable.userAction.actionIntent.intentSender).build())
            } else {
                viewModel.refresh()
                selectedUris = emptySet()
                if (count > 0) {
                    Toast.makeText(context, "$count items deleted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun performShare(itemsToShare: List<MediaItem>) {
        if (itemsToShare.isEmpty()) return
        val uris = ArrayList(itemsToShare.map { it.uri })
        val intent = if (uris.size == 1) {
            val single = itemsToShare.first()
            Intent(Intent.ACTION_SEND).apply {
                type = single.mimeType
                putExtra(Intent.EXTRA_STREAM, single.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val allVideos = itemsToShare.all { it.isVideo }
            val allImages = itemsToShare.all { !it.isVideo }
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (allVideos) "video/*" else if (allImages) "image/*" else "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share media"))
    }

    if (showDeleteConfirmDialog) {
        AppDialog(
            isOpen = showDeleteConfirmDialog,
            title = "Move ${selectedItems.size} items to Recycle Bin?",
            message = "You can restore them from the Recycle Bin within 30 days.",
            confirmLabel = "Move to Bin",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmDialog = false
                performDelete(selectedItems)
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    if (showDetailsDialog) {
        val photoCount = selectedItems.count { !it.isVideo }
        val videoCount = selectedItems.count { it.isVideo }
        AppDialog(
            isOpen = showDetailsDialog,
            title = "Selection Details",
            message = buildString {
                append("Items: ${selectedItems.size} total")
                if (photoCount > 0) append(" ($photoCount photos")
                if (videoCount > 0) append(if (photoCount > 0) ", $videoCount videos)" else " ($videoCount videos)")
                else if (photoCount > 0) append(")")
                append("\nTotal Size: $formattedSelectedSize")
                val dates = selectedItems.mapNotNull { if (it.formattedDate.isNotEmpty()) it.formattedDate else null }.distinct()
                if (dates.isNotEmpty()) {
                    append("\nDates: ${dates.first()}${if (dates.size > 1) " – ${dates.last()}" else ""}")
                }
            },
            confirmLabel = "OK",
            onConfirm = { showDetailsDialog = false },
            onDismiss = { showDetailsDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AnimatedContent(
            targetState = isSelectionMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 })
                .togetherWith(fadeOut(animationSpec = tween(140)) + slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 })
            },
            label = "mediaTopBarMode"
        ) { selectionActive ->
            if (selectionActive) {
                // Contextual multi-selection top bar
                Surface(
                    color = colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { selectedUris = emptySet() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close selection",
                                    tint = colors.textPrimary
                                )
                            }
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = "${selectedUris.size} selected",
                                    style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = formattedSelectedSize,
                                    style = typography.caption.copy(fontSize = 11.sp),
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val allSelected = allVisibleItems.isNotEmpty() && selectedUris.size == allVisibleItems.size
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedUris = if (allSelected) {
                                        emptySet()
                                    } else {
                                        allVisibleItems.map { it.uri }.toSet()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Outlined.Deselect else Icons.Outlined.SelectAll,
                                    contentDescription = if (allSelected) "Deselect All" else "Select All",
                                    tint = colors.textPrimary
                                )
                            }

                            IconButton(onClick = { showDetailsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Selection Details",
                                    tint = colors.textPrimary
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    performShare(selectedItems)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Share",
                                    tint = colors.textPrimary
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDeleteConfirmDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = colors.error
                                )
                            }
                        }
                    }
                }
            } else {
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
                    if (allVisibleItems.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Select items", style = typography.body) },
                            leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.accent) },
                            onClick = {
                                onDismiss()
                                selectedUris = setOf(allVisibleItems.first().uri)
                            }
                        )
                    }
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
                        val mediaCategories = listOf(
                            MediaNavCategory(MediaFilter.ALL, Icons.Outlined.PermMedia, colors.accent, "All"),
                            MediaNavCategory(MediaFilter.PHOTOS, Icons.Outlined.Image, Color(0xFF10B981), "Photos"),
                            MediaNavCategory(MediaFilter.VIDEOS, Icons.Outlined.Videocam, DocumentFormatColors.mediaTint, "Videos")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (item in mediaCategories) {
                                val isSelected = uiState.filter == item.filter
                                val chipBg = if (isSelected) item.color.copy(alpha = 0.16f) else colors.surfaceContainer
                                val chipBorderColor = if (isSelected) item.color else colors.cardBorder

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = chipBg,
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, chipBorderColor),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setFilter(item.filter)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) item.color else item.color.copy(alpha = 0.85f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.label,
                                            style = typography.caption.copy(
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else null
            )
        }
    }

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
                        start = AppSpacing.paddingSmall,
                        end = AppSpacing.paddingSmall,
                        top = AppSpacing.paddingSmall,
                        bottom = 104.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    displayedGroups.forEach { group ->
                        item(
                            key = "header_${group.title}",
                            span = { GridItemSpan(3) }
                        ) {
                            AppSectionHeader(
                                title = group.title
                            )
                        }

                        items(
                            items = group.items,
                            key = { it.uri.toString() },
                            contentType = { "media_thumb" }
                        ) { item ->
                            val isItemSelected = selectedUris.contains(item.uri)
                            MediaThumbnailItem(
                                item = item,
                                isSelected = isItemSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedUris = if (isItemSelected) {
                                            selectedUris - item.uri
                                        } else {
                                            selectedUris + item.uri
                                        }
                                    } else {
                                        val relevantList = if (item.isVideo) allVideoItems else allPhotoItems
                                        onOpenMedia(item, relevantList)
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedUris = if (isItemSelected) {
                                        selectedUris - item.uri
                                    } else {
                                        selectedUris + item.uri
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaThumbnailItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val typography = LocalAppTypography.current
    val colors = LocalAppColors.current
    val isOled = colors.background == DarkAppColors.background

    val imageRequest = remember(item.uri, item.isVideo) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .apply {
                if (item.isVideo) {
                    videoFramePercent(0.1)
                    videoFrameOption(MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            }
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .border(
                BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) colors.accent else colors.cardBorder
                ),
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Tint overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.accent.copy(alpha = 0.28f))
            )
        }

        // Selection bubble in top-right corner
        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.accent else Color.Black.copy(alpha = 0.55f))
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) colors.accent else Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Play icon in center if video and not in selection mode
        if (item.isVideo && !isSelectionMode) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0x66FFFFFF),
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
            )
        }

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
                        style = typography.caption.withTabularFigures().copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = Color.White
                    )
                }
            }
        }
    }
}
