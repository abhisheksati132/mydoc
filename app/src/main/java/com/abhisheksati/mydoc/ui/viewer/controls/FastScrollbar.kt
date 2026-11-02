package com.abhisheksati.mydoc.ui.viewer.controls

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Draggable fast scrollbar positioned on the right edge of the viewer.
 * Displays floating thumbnail preview and page indicator with haptic tick during dragging.
 */
@Composable
fun FastScrollbar(
    currentPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    renderThumbnail: (suspend (pageIndex: Int, scale: Float) -> Bitmap?)? = null,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return

    val haptics = com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics()
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var lastHapticPage by remember { mutableIntStateOf(currentPage) }
    var liveThumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(isDragging, lastHapticPage) {
        if (isDragging && renderThumbnail != null) {
            liveThumbnail = renderThumbnail(lastHapticPage, 0.45f)
        } else if (!isDragging) {
            liveThumbnail = null
        }
    }

    // Auto fade bubble after drag ends
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            delay(1200)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(AppSpacing.minTouchTarget)
    ) {
        val availableHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightDp = 48.dp
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val maxTravelPx = (availableHeightPx - thumbHeightPx).coerceAtLeast(1f)

        // Compute current thumb Y
        val currentProgress = if (isDragging) {
            dragProgress
        } else {
            (currentPage.toFloat() / (pageCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
        val thumbYOffsetPx = currentProgress * maxTravelPx

        // Draggable gesture detector covering full 48dp touch column
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pageCount, availableHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val normalized = (offset.y / availableHeightPx).coerceIn(0f, 1f)
                            dragProgress = normalized
                            val targetPage = (normalized * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
                            if (targetPage != lastHapticPage) {
                                haptics.tick()
                                lastHapticPage = targetPage
                            }
                            onPageSelected(targetPage)
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val normalized = (change.position.y / availableHeightPx).coerceIn(0f, 1f)
                            dragProgress = normalized
                            val targetPage = (normalized * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
                            if (targetPage != lastHapticPage) {
                                haptics.tick()
                                lastHapticPage = targetPage
                            }
                            onPageSelected(targetPage)
                        }
                    )
                }
        ) {
            // Track line (subtle divider)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )

            // Scroll thumb (48dp height)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = 0, y = thumbYOffsetPx.roundToInt()) }
                    .padding(end = 2.dp)
                    .size(width = 6.dp, height = thumbHeightDp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isDragging) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
            )

            // Floating page bubble with live thumbnail preview during drag
            AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = -with(density) { (if (liveThumbnail != null) 92.dp else 60.dp).roundToPx() },
                            y = (thumbYOffsetPx - with(density) { (if (liveThumbnail != null) 54.dp else 12.dp).roundToPx() }).roundToInt()
                                .coerceIn(0, (availableHeightPx - with(density) { 120.dp.roundToPx() }).roundToInt().coerceAtLeast(0))
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (liveThumbnail != null) {
                        Image(
                            bitmap = liveThumbnail!!.asImageBitmap(),
                            contentDescription = "Page ${lastHapticPage + 1}",
                            modifier = Modifier
                                .size(width = 68.dp, height = 90.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = "${lastHapticPage + 1} / $pageCount",
                        style = MaterialTheme.typography.labelMedium.withTabularFigures(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
