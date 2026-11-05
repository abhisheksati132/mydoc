package com.abhisheksati.mydoc.ui.media

import android.content.Intent
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenuItem
import com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.RectangleShape
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.rememberGatedHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.domain.model.ExifMetadata
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.abhisheksati.mydoc.designsystem.components.showMessage
import com.abhisheksati.mydoc.util.openFile
import com.abhisheksati.mydoc.util.shareFile
import com.abhisheksati.mydoc.util.toShareableUri

@Composable
fun PhotoViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeletePhoto: ((MediaItem) -> Unit)? = null
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = rememberGatedHaptics()
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
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf<String?>(null) }
    val ocrEngine = remember { com.abhisheksati.mydoc.ocr.OcrEngine(context) }
    DisposableEffect(ocrEngine) {
        onDispose {
            ocrEngine.close()
        }
    }

    // The viewer is an overlay, not a nav destination: back must close it, not pop the tab.
    BackHandler(enabled = true) { onBackClick() }

    // Reset rotation when swiping to another photo.
    LaunchedEffect(pagerState.currentPage) {
        rotationAngle = 0f
    }

    // Query EXIF on-demand for current photo
    @Suppress("ProduceStateDoesNotAssignValue")
    val currentExif by produceState<ExifMetadata?>(initialValue = null, currentItem.uri) {
        value = mediaRepository.extractExif(currentItem.uri)
    }

    val isDarkMode = LocalIsOled.current
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
                    onTap = { areControlsVisible = !areControlsVisible }
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
            GlassSurface(
                shape = RectangleShape,
                elevation = 8.dp,
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
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                            style = typography.documentName,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (photoList.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1} of ${photoList.size}",
                                style = typography.caption.withTabularFigures(),
                                color = secondaryColor
                            )
                        }
                    }

                    // Info lives in the bottom pill row; the top bar carries identity only.

                    Box {
                        IconButton(onClick = { isMoreMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = contentColor
                            )
                        }

                        AppDropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false }
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
                                text = { Text("Open with...", color = colors.textPrimary) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = colors.textPrimary)
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    if (!openFile(context, currentItem.uri, currentItem.mimeType)) {
                                        context.showMessage("No app found to open this image")
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
                                    val safeUri = currentItem.uri.toShareableUri(context)
                                    if (safeUri == null) {
                                        context.showMessage("Unable to set wallpaper")
                                    } else {
                                        val setAsIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                            setDataAndType(safeUri, currentItem.mimeType)
                                            putExtra("mimeType", currentItem.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(setAsIntent, "Set as"))
                                        } catch (_: Exception) {
                                            context.showMessage("Unable to set wallpaper")
                                        }
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
            GlassSurface(
                shape = RectangleShape,
                elevation = 8.dp,
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
                                    shape = AppRadius.cardShape,
                                    border = if (isSelected) BorderStroke(2.dp, colors.accent) else null,
                                    color = colors.surfaceMuted,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(AppRadius.cardShape)
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
                                if (!shareFile(context, currentItem.uri, currentItem.mimeType, "Share image")) {
                                    context.showMessage("No app available to share this image")
                                }
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
                text = ocrExtractedText ?: "",
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
                        val trashTarget = file?.let { f -> app?.let { a -> f to a } }
                        val recoverable = trashTarget != null
                        val success = withContext(Dispatchers.IO) {
                            val done = if (trashTarget != null) {
                                trashTarget.second.storageRepository
                                    .moveToTrash(listOf(trashTarget.first)).isSuccess
                            } else {
                                try {
                                    context.contentResolver.delete(itemToDelete.uri, null, null) > 0
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            if (done) app?.mediaRepository?.invalidateMediaSnapshot()
                            done
                        }
                        if (success) {
                            context.showMessage(
                                if (recoverable) "Photo moved to Recycle Bin"
                                else "Photo deleted. MyDoc could not move it to the Recycle Bin."
                            )
                            onDeletePhoto?.invoke(itemToDelete)
                            val idx = photoList.indexOf(itemToDelete)
                            if (idx >= 0) {
                                photoList.removeAt(idx)
                            }
                            if (photoList.isEmpty()) {
                                onBackClick()
                            }
                        } else {
                            context.showMessage("Failed to move photo to Recycle Bin")
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
    Surface(
        shape = AppRadius.xLargeShape,
        color = LocalAppColors.current.surfaceMuted,
        modifier = modifier
            .height(56.dp)
            .clip(AppRadius.xLargeShape)
            .clickable(onClick = {
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
                style = LocalAppTypography.current.captionStrong,
                color = tint
            )
        }
    }
}

