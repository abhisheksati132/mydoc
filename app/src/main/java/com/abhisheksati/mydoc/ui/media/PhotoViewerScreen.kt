package com.abhisheksati.mydoc.ui.media

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.ExifMetadata
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PhotoViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit,
    onDeletePhoto: ((MediaItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val photoList = remember(items) { mutableStateListOf(*items.toTypedArray()) }
    if (photoList.isEmpty()) {
        onBackClick()
        return
    }

    val safeInitialIndex = initialIndex.coerceIn(0, (photoList.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { photoList.size }
    )

    val currentItem = photoList.getOrNull(pagerState.currentPage.coerceIn(0, (photoList.size - 1).coerceAtLeast(0))) ?: photoList.firstOrNull()
    if (currentItem == null) {
        onBackClick()
        return
    }

    var areControlsVisible by remember { mutableStateOf(true) }
    var isInfoSheetOpen by remember { mutableStateOf(false) }
    var isFillScale by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var isSlideshowActive by remember { mutableStateOf(false) }
    var slideshowIntervalSec by remember { mutableIntStateOf(3) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf<String?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    val ocrEngine = remember { com.abhisheksati.mydoc.ocr.OcrEngine(context) }

    // Intercept back key to exit slideshow first or close photo viewer
    BackHandler {
        if (isSlideshowActive) {
            isSlideshowActive = false
        } else {
            onBackClick()
        }
    }

    // Reset rotation and flip when swiping to another photo
    LaunchedEffect(pagerState.currentPage) {
        rotationAngle = 0f
        isFlippedHorizontal = false
    }

    // Slideshow auto-advance timer
    LaunchedEffect(isSlideshowActive, pagerState.currentPage, slideshowIntervalSec, photoList.size) {
        if (isSlideshowActive && photoList.isNotEmpty()) {
            areControlsVisible = false
            delay(slideshowIntervalSec * 1000L)
            val next = (pagerState.currentPage + 1) % photoList.size
            pagerState.animateScrollToPage(next)
        }
    }

    // Query EXIF on-demand for current photo
    @Suppress("ProduceStateDoesNotAssignValue")
    val currentExif by produceState<ExifMetadata?>(initialValue = null, currentItem.uri) {
        value = mediaRepository.extractExif(currentItem.uri)
    }

    val isDarkMode = colors.background == com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors.background
    val viewerBackground = colors.background
    val overlayBackground = if (isDarkMode) colors.surface.copy(alpha = 0.88f) else colors.surface.copy(alpha = 0.94f)
    val contentColor = colors.textPrimary
    val secondaryColor = colors.textSecondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(viewerBackground)
    ) {
        // Image Pager
        HorizontalPager(
            state = pagerState,
            key = { photoList.getOrNull(it)?.uri?.toString() ?: it.toString() },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = photoList.getOrNull(page)
            if (item != null) {
                ZoomableImage(
                    item = item,
                    isFillScale = isFillScale,
                    displayRotation = if (page == pagerState.currentPage) rotationAngle else 0f,
                    isFlippedHorizontal = if (page == pagerState.currentPage) isFlippedHorizontal else false,
                    onTap = {
                        if (isSlideshowActive) {
                            isSlideshowActive = false
                            areControlsVisible = true
                        } else {
                            areControlsVisible = !areControlsVisible
                        }
                    }
                )
            }
        }

        // Top Toolbar Overlay
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = overlayBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = AppSpacing.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = contentColor
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = AppSpacing.paddingSmall)
                    ) {
                        Text(
                            text = currentItem.displayName,
                            style = typography.body.copy(fontWeight = FontWeight.Medium),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (photoList.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1} of ${photoList.size}",
                                style = typography.caption.copy(fontFeatureSettings = "tnum"),
                                color = secondaryColor
                            )
                        }
                    }

                    IconButton(onClick = { isInfoSheetOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = contentColor
                        )
                    }

                    Box {
                        IconButton(onClick = { isMoreMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = contentColor
                            )
                        }

                        DropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = if (isDarkMode) Color(0xFF1E2129) else colors.surface,
                            border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.12f) else colors.cardBorder),
                            shadowElevation = 8.dp,
                            tonalElevation = 4.dp
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rotate 90°", color = colors.textPrimary) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = colors.textPrimary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    rotationAngle = (rotationAngle + 90f) % 360f
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Flip horizontal", color = colors.textPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Default.Flip, contentDescription = null, tint = colors.textPrimary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    isFlippedHorizontal = !isFlippedHorizontal
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isSlideshowActive) "Stop slideshow" else "Slideshow (3s)", color = colors.textPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.textPrimary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    isSlideshowActive = !isSlideshowActive
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open with...", color = colors.textPrimary) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.textPrimary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(currentItem.uri, currentItem.mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(openIntent, "Open with"))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No app found to open this image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set as wallpaper", color = colors.textPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Image, contentDescription = null, tint = colors.textPrimary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    val setAsIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                        setDataAndType(currentItem.uri, currentItem.mimeType)
                                        putExtra("mimeType", currentItem.mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(setAsIntent, "Set as"))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to set wallpaper", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Recycle Bin", color = colors.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = colors.error
                                    )
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls Overlay with Filmstrip & 56dp labelled action pills
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = overlayBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 6.dp)
                ) {
                    // Filmstrip of neighbours
                    if (photoList.size > 1) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(photoList.size) { idx ->
                                val p = photoList[idx]
                                val isSelected = idx == pagerState.currentPage
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (isSelected) BorderStroke(2.dp, colors.accent) else null,
                                    color = colors.surfaceMuted,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(idx)
                                            }
                                        }
                                ) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(p.uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // 4 labelled 56dp pills: Share, Edit, Info, Delete
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share
                        PhotoViewerPillButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            tint = contentColor,
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = currentItem.mimeType
                                    putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share image"))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Rotate 90°
                        PhotoViewerPillButton(
                            icon = Icons.AutoMirrored.Filled.RotateRight,
                            label = "Rotate",
                            tint = contentColor,
                            onClick = {
                                rotationAngle = (rotationAngle + 90f) % 360f
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Info
                        PhotoViewerPillButton(
                            icon = Icons.Default.Info,
                            label = "Info",
                            tint = contentColor,
                            onClick = { isInfoSheetOpen = true },
                            modifier = Modifier.weight(1f)
                        )

                        // Delete
                        PhotoViewerPillButton(
                            icon = Icons.Outlined.Delete,
                            label = "Delete",
                            tint = colors.error,
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Photo Info Bottom Sheet
        PhotoInfoSheet(
            item = currentItem,
            exif = currentExif,
            isOpen = isInfoSheetOpen,
            onDismiss = { isInfoSheetOpen = false }
        )

        // OCR Result Modal Bottom Sheet
        if (ocrExtractedText != null) {
            com.abhisheksati.mydoc.ocr.OcrResultSheet(
                text = ocrExtractedText!!,
                onDismiss = { ocrExtractedText = null }
            )
        }



        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AppDialog(
                isOpen = showDeleteConfirmDialog,
                title = "Move to Recycle Bin?",
                message = "Are you sure you want to move \"${currentItem.displayName}\" to the Recycle Bin?",
                confirmLabel = "Move to Bin",
                isDestructive = true,
                onConfirm = {
                    showDeleteConfirmDialog = false
                    val itemToDelete = currentItem
                    coroutineScope.launch {
                        val app = context.applicationContext as? MyDocApp
                        val file = itemToDelete.filePath?.let { File(it).takeIf { f -> f.exists() } }
                        val success = withContext(Dispatchers.IO) {
                            if (file != null && app != null) {
                                app.storageRepository.moveToTrash(listOf(file)).isSuccess
                            } else {
                                try {
                                    context.contentResolver.delete(itemToDelete.uri, null, null) > 0
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }
                        if (success) {
                            Toast.makeText(context, "Photo moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            onDeletePhoto?.invoke(itemToDelete)
                            val idx = photoList.indexOf(itemToDelete)
                            if (idx >= 0) {
                                photoList.removeAt(idx)
                            }
                            if (photoList.isEmpty()) {
                                onBackClick()
                            }
                        } else {
                            Toast.makeText(context, "Failed to move photo to Recycle Bin", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                dismissLabel = "Cancel",
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }
    }
}

@Composable
private fun PhotoViewerPillButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = LocalAppColors.current.surfaceMuted,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            })
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = LocalAppTypography.current.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = tint
            )
        }
    }
}

