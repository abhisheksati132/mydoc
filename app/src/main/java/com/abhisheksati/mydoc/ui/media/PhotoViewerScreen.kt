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
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.ExifMetadata
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.domain.repository.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PhotoViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val typography = LocalAppTypography.current
    val coroutineScope = rememberCoroutineScope()

    val safeInitialIndex = initialIndex.coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { items.size }
    )

    val currentItem = items.getOrNull(pagerState.currentPage) ?: items[0]

    var areControlsVisible by remember { mutableStateOf(true) }
    var isInfoSheetOpen by remember { mutableStateOf(false) }
    var isFillScale by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var isSlideshowActive by remember { mutableStateOf(false) }
    var slideshowIntervalSec by remember { mutableIntStateOf(3) }

    // Intercept back key to exit slideshow first or close photo viewer
    BackHandler {
        if (isSlideshowActive) {
            isSlideshowActive = false
        } else {
            onBackClick()
        }
    }

    // Reset rotation when swiping to another photo
    LaunchedEffect(pagerState.currentPage) {
        rotationAngle = 0f
    }

    // Slideshow auto-advance timer
    LaunchedEffect(isSlideshowActive, pagerState.currentPage, slideshowIntervalSec) {
        if (isSlideshowActive) {
            areControlsVisible = false
            delay(slideshowIntervalSec * 1000L)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    // Query EXIF on-demand for current photo
    val currentExif by produceState<ExifMetadata?>(initialValue = null, currentItem.uri) {
        value = mediaRepository.extractExif(currentItem.uri)
    }

    val darkBackground = Color(0xFF0F0F10) // Soft near-black token style
    val overlayBackground = Color(0xD9161618)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkBackground)
    ) {
        // Image Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            ZoomableImage(
                item = item,
                isFillScale = isFillScale,
                displayRotation = if (page == pagerState.currentPage) rotationAngle else 0f,
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
                            tint = Color(0xFFEDEDED)
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
                            color = Color(0xFFEDEDED),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (items.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1} of ${items.size}",
                                style = typography.caption,
                                color = Color(0xFFA0A0A5)
                            )
                        }
                    }

                    IconButton(onClick = { isInfoSheetOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFFEDEDED)
                        )
                    }

                    Box {
                        IconButton(onClick = { isMoreMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color(0xFFEDEDED)
                            )
                        }

                        DropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isSlideshowActive) "Stop slideshow" else "Slideshow (3s)") },
                                onClick = {
                                    isMoreMenuOpen = false
                                    isSlideshowActive = !isSlideshowActive
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open with...") },
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
                                text = { Text("Set as wallpaper") },
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
                        }
                    }
                }
            }
        }

        // Bottom Controls Overlay
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = AppSpacing.paddingLarge),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate
                    IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate",
                            tint = Color(0xFFEDEDED)
                        )
                    }

                    // Fit/Fill toggle
                    IconButton(onClick = { isFillScale = !isFillScale }) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = if (isFillScale) "Fit to screen" else "Fill screen",
                            tint = if (isFillScale) Color(0xFF8AB4F8) else Color(0xFFEDEDED)
                        )
                    }

                    // Slideshow toggle
                    IconButton(onClick = { isSlideshowActive = !isSlideshowActive }) {
                        Icon(
                            imageVector = if (isSlideshowActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isSlideshowActive) "Stop" else "Slideshow",
                            tint = if (isSlideshowActive) Color(0xFF8AB4F8) else Color(0xFFEDEDED)
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = currentItem.mimeType
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share image"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFFEDEDED)
                        )
                    }
                }
            }
        }

        // Info Bottom Sheet
        PhotoInfoSheet(
            item = currentItem,
            exif = currentExif,
            isOpen = isInfoSheetOpen,
            onDismiss = { isInfoSheetOpen = false }
        )
    }
}
