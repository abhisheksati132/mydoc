package com.abhisheksati.mydoc.tools.scan.cropper

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RestartAlt
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class DragHandle {
    NONE,
    CORNER_TL,
    CORNER_TR,
    CORNER_BR,
    CORNER_BL,
    EDGE_TOP,
    EDGE_RIGHT,
    EDGE_BOTTOM,
    EDGE_LEFT,
    BODY
}

@Composable
fun FreeCornersCropperDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit,
    accentColor: Color = Color(0xFF4A90E2),
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        var currentBitmap by remember { mutableStateOf(bitmap) }

        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color(0xFF0F0F12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Perspective Deskew",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = {
                            // Rotate 90 degrees
                            val matrix = Matrix().apply { postRotate(90f) }
                            currentBitmap = Bitmap.createBitmap(
                                currentBitmap, 0, 0,
                                currentBitmap.width, currentBitmap.height,
                                matrix, true
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = Color.White
                        )
                    }
                }

                // Interactive Crop Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FreeCornersCropperCanvas(
                        bitmap = currentBitmap,
                        accentColor = accentColor,
                        onCropped = onCropped
                    )
                }
            }
        }
    }
}

@Composable
private fun FreeCornersCropperCanvas(
    bitmap: Bitmap,
    accentColor: Color,
    onCropped: (Bitmap) -> Unit
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val handleTouchRadiusPx = with(density) { 36.dp.toPx() }
    val cornerCircleRadiusPx = with(density) { 11.dp.toPx() }
    val strokeWidthPx = with(density) { 2.2.dp.toPx() }
    val gridStrokeWidthPx = with(density) { 1.dp.toPx() }
    val snapThresholdPx = with(density) { 14.dp.toPx() }
    var wasSnapped by remember { mutableStateOf(false) }

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageBounds by remember { mutableStateOf(Rect.Zero) }
    var scaleFactor by remember { mutableFloatStateOf(1f) }

    // 4 Quad Corners in Screen Display Coordinates: [TL, TR, BR, BL]
    val quadPoints = remember { mutableStateListOf(Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero) }
    var isInitialized by remember(bitmap) { mutableStateOf(false) }

    // Drag tracking state
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    var dragTouchPosition by remember { mutableStateOf(Offset.Unspecified) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        if (maxWidthPx > 0 && maxHeightPx > 0) {
            val bmpW = bitmap.width.toFloat()
            val bmpH = bitmap.height.toFloat()

            val fitScale = minOf(maxWidthPx / bmpW, maxHeightPx / bmpH) * 0.92f
            val displayW = bmpW * fitScale
            val displayH = bmpH * fitScale

            val left = (maxWidthPx - displayW) / 2f
            val top = (maxHeightPx - displayH) / 2f

            imageBounds = Rect(left, top, left + displayW, top + displayH)
            scaleFactor = fitScale

            if (!isInitialized) {
                // Automatically detect document boundary edges
                val autoQuad = DocumentEdgeDetector.detectDocumentQuad(bitmap)
                if (autoQuad != null) {
                    quadPoints[0] = Offset(left + autoQuad.topLeft.x * fitScale, top + autoQuad.topLeft.y * fitScale)
                    quadPoints[1] = Offset(left + autoQuad.topRight.x * fitScale, top + autoQuad.topRight.y * fitScale)
                    quadPoints[2] = Offset(left + autoQuad.bottomRight.x * fitScale, top + autoQuad.bottomRight.y * fitScale)
                    quadPoints[3] = Offset(left + autoQuad.bottomLeft.x * fitScale, top + autoQuad.bottomLeft.y * fitScale)
                } else {
                    // Fallback: Initialize quad with a subtle 5% inset
                    val insetX = displayW * 0.05f
                    val insetY = displayH * 0.05f
                    quadPoints[0] = Offset(left + insetX, top + insetY)
                    quadPoints[1] = Offset(left + displayW - insetX, top + insetY)
                    quadPoints[2] = Offset(left + displayW - insetX, top + displayH - insetY)
                    quadPoints[3] = Offset(left + insetX, top + displayH - insetY)
                }
                isInitialized = true
            }
        }

        // Gesture Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, imageBounds) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val p0 = quadPoints[0]
                            val p1 = quadPoints[1]
                            val p2 = quadPoints[2]
                            val p3 = quadPoints[3]

                            // Check corners first (highest priority)
                            val d0 = hypot(startOffset.x - p0.x, startOffset.y - p0.y)
                            val d1 = hypot(startOffset.x - p1.x, startOffset.y - p1.y)
                            val d2 = hypot(startOffset.x - p2.x, startOffset.y - p2.y)
                            val d3 = hypot(startOffset.x - p3.x, startOffset.y - p3.y)

                            activeHandle = when {
                                d0 < handleTouchRadiusPx -> DragHandle.CORNER_TL
                                d1 < handleTouchRadiusPx -> DragHandle.CORNER_TR
                                d2 < handleTouchRadiusPx -> DragHandle.CORNER_BR
                                d3 < handleTouchRadiusPx -> DragHandle.CORNER_BL
                                else -> {
                                    // Check edge midpoints
                                    val midTop = (p0 + p1) / 2f
                                    val midRight = (p1 + p2) / 2f
                                    val midBottom = (p2 + p3) / 2f
                                    val midLeft = (p3 + p0) / 2f

                                    val dmTop = hypot(startOffset.x - midTop.x, startOffset.y - midTop.y)
                                    val dmRight = hypot(startOffset.x - midRight.x, startOffset.y - midRight.y)
                                    val dmBottom = hypot(startOffset.x - midBottom.x, startOffset.y - midBottom.y)
                                    val dmLeft = hypot(startOffset.x - midLeft.x, startOffset.y - midLeft.y)

                                    when {
                                        dmTop < handleTouchRadiusPx -> DragHandle.EDGE_TOP
                                        dmRight < handleTouchRadiusPx -> DragHandle.EDGE_RIGHT
                                        dmBottom < handleTouchRadiusPx -> DragHandle.EDGE_BOTTOM
                                        dmLeft < handleTouchRadiusPx -> DragHandle.EDGE_LEFT
                                        else -> DragHandle.NONE
                                    }
                                }
                            }
                            if (activeHandle != DragHandle.NONE) {
                                haptics.select()
                            }
                            wasSnapped = false
                            dragTouchPosition = startOffset
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragTouchPosition = change.position

                            fun snapAndClamp(
                                offset: Offset,
                                hRef: Float? = null,
                                vRef: Float? = null
                            ): Offset {
                                var x = offset.x.coerceIn(imageBounds.left, imageBounds.right)
                                var y = offset.y.coerceIn(imageBounds.top, imageBounds.bottom)
                                var snapped = false

                                // Magnetic snap to outer image bounds
                                if (abs(x - imageBounds.left) <= snapThresholdPx) {
                                    x = imageBounds.left
                                    snapped = true
                                } else if (abs(x - imageBounds.right) <= snapThresholdPx) {
                                    x = imageBounds.right
                                    snapped = true
                                }

                                if (abs(y - imageBounds.top) <= snapThresholdPx) {
                                    y = imageBounds.top
                                    snapped = true
                                } else if (abs(y - imageBounds.bottom) <= snapThresholdPx) {
                                    y = imageBounds.bottom
                                    snapped = true
                                }

                                // Magnetic snap to orthogonal alignment with adjacent corners
                                if (hRef != null && abs(x - hRef) <= snapThresholdPx) {
                                    x = hRef
                                    snapped = true
                                }
                                if (vRef != null && abs(y - vRef) <= snapThresholdPx) {
                                    y = vRef
                                    snapped = true
                                }

                                if (snapped && !wasSnapped) {
                                    haptics.snap()
                                }
                                wasSnapped = snapped

                                return Offset(x, y)
                            }

                            when (activeHandle) {
                                DragHandle.CORNER_TL -> quadPoints[0] = snapAndClamp(quadPoints[0] + dragAmount, hRef = quadPoints[3].x, vRef = quadPoints[1].y)
                                DragHandle.CORNER_TR -> quadPoints[1] = snapAndClamp(quadPoints[1] + dragAmount, hRef = quadPoints[2].x, vRef = quadPoints[0].y)
                                DragHandle.CORNER_BR -> quadPoints[2] = snapAndClamp(quadPoints[2] + dragAmount, hRef = quadPoints[1].x, vRef = quadPoints[3].y)
                                DragHandle.CORNER_BL -> quadPoints[3] = snapAndClamp(quadPoints[3] + dragAmount, hRef = quadPoints[0].x, vRef = quadPoints[2].y)
                                DragHandle.EDGE_TOP -> {
                                    var newY = (quadPoints[0].y + dragAmount.y).coerceIn(imageBounds.top, imageBounds.bottom)
                                    var snapped = false
                                    if (abs(newY - imageBounds.top) <= snapThresholdPx) {
                                        newY = imageBounds.top
                                        snapped = true
                                    }
                                    if (snapped && !wasSnapped) {
                                        haptics.snap()
                                    }
                                    wasSnapped = snapped
                                    quadPoints[0] = Offset(quadPoints[0].x, newY)
                                    quadPoints[1] = Offset(quadPoints[1].x, newY)
                                }
                                DragHandle.EDGE_RIGHT -> {
                                    var newX = (quadPoints[1].x + dragAmount.x).coerceIn(imageBounds.left, imageBounds.right)
                                    var snapped = false
                                    if (abs(newX - imageBounds.right) <= snapThresholdPx) {
                                        newX = imageBounds.right
                                        snapped = true
                                    }
                                    if (snapped && !wasSnapped) {
                                        haptics.snap()
                                    }
                                    wasSnapped = snapped
                                    quadPoints[1] = Offset(newX, quadPoints[1].y)
                                    quadPoints[2] = Offset(newX, quadPoints[2].y)
                                }
                                DragHandle.EDGE_BOTTOM -> {
                                    var newY = (quadPoints[2].y + dragAmount.y).coerceIn(imageBounds.top, imageBounds.bottom)
                                    var snapped = false
                                    if (abs(newY - imageBounds.bottom) <= snapThresholdPx) {
                                        newY = imageBounds.bottom
                                        snapped = true
                                    }
                                    if (snapped && !wasSnapped) {
                                        haptics.snap()
                                    }
                                    wasSnapped = snapped
                                    quadPoints[2] = Offset(quadPoints[2].x, newY)
                                    quadPoints[3] = Offset(quadPoints[3].x, newY)
                                }
                                DragHandle.EDGE_LEFT -> {
                                    var newX = (quadPoints[3].x + dragAmount.x).coerceIn(imageBounds.left, imageBounds.right)
                                    var snapped = false
                                    if (abs(newX - imageBounds.left) <= snapThresholdPx) {
                                        newX = imageBounds.left
                                        snapped = true
                                    }
                                    if (snapped && !wasSnapped) {
                                        haptics.snap()
                                    }
                                    wasSnapped = snapped
                                    quadPoints[3] = Offset(newX, quadPoints[3].y)
                                    quadPoints[0] = Offset(newX, quadPoints[0].y)
                                }
                                else -> {}
                            }
                        },
                        onDragEnd = {
                            activeHandle = DragHandle.NONE
                            dragTouchPosition = Offset.Unspecified
                            wasSnapped = false
                        },
                        onDragCancel = {
                            activeHandle = DragHandle.NONE
                            dragTouchPosition = Offset.Unspecified
                            wasSnapped = false
                        }
                    )
                }
        ) {
            if (imageBounds.width > 0 && imageBounds.height > 0) {
                // 1. Draw image within imageBounds
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(imageBounds.left.roundToInt(), imageBounds.top.roundToInt()),
                    dstSize = IntSize(imageBounds.width.roundToInt(), imageBounds.height.roundToInt())
                )

                // 2. Draw Dimmed Scrim outside quad
                val fullPath = Path().apply {
                    addRect(imageBounds)
                }
                val quadPath = Path().apply {
                    moveTo(quadPoints[0].x, quadPoints[0].y)
                    lineTo(quadPoints[1].x, quadPoints[1].y)
                    lineTo(quadPoints[2].x, quadPoints[2].y)
                    lineTo(quadPoints[3].x, quadPoints[3].y)
                    close()
                }

                val scrimPath = Path.combine(
                    operation = PathOperation.Difference,
                    path1 = fullPath,
                    path2 = quadPath
                )
                drawPath(scrimPath, color = Color.Black.copy(alpha = 0.55f))

                // 3. Draw Perspective Grid
                drawPerspectiveGrid(
                    points = quadPoints.toList(),
                    color = Color.White.copy(alpha = 0.45f),
                    strokeWidth = gridStrokeWidthPx
                )

                // 4. Draw Quad Border
                drawPath(
                    path = quadPath,
                    color = accentColor,
                    style = Stroke(width = strokeWidthPx)
                )

                // 5. Draw Edge Midpoint Handle Pills
                val midTop = (quadPoints[0] + quadPoints[1]) / 2f
                val midRight = (quadPoints[1] + quadPoints[2]) / 2f
                val midBottom = (quadPoints[2] + quadPoints[3]) / 2f
                val midLeft = (quadPoints[3] + quadPoints[0]) / 2f

                listOf(midTop, midRight, midBottom, midLeft).forEach { mid ->
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = mid)
                    drawCircle(color = accentColor, radius = 3.dp.toPx(), center = mid)
                }

                // 6. Draw 4 Corner Handles
                quadPoints.forEach { pt ->
                    // Outer white ring
                    drawCircle(
                        color = Color.White,
                        radius = cornerCircleRadiusPx,
                        center = pt
                    )
                    // Inner colored center
                    drawCircle(
                        color = accentColor,
                        radius = cornerCircleRadiusPx - 2.5f,
                        center = pt
                    )
                }
            }
        }

        // Live Magnifier Loupe during handle drag
        if (activeHandle != DragHandle.NONE && dragTouchPosition != Offset.Unspecified && scaleFactor > 0f) {
            val loupeRadius = 52.dp
            val loupeDiameter = loupeRadius * 2
            val targetCorner = when (activeHandle) {
                DragHandle.CORNER_TL -> quadPoints[0]
                DragHandle.CORNER_TR -> quadPoints[1]
                DragHandle.CORNER_BR -> quadPoints[2]
                DragHandle.CORNER_BL -> quadPoints[3]
                else -> dragTouchPosition
            }

            // Position loupe safely above finger (or below if near top edge)
            val loupeOffsetY = if (targetCorner.y > with(density) { 150.dp.toPx() }) {
                targetCorner.y - with(density) { 95.dp.toPx() }
            } else {
                targetCorner.y + with(density) { 95.dp.toPx() }
            }
            val loupeOffsetX = targetCorner.x.coerceIn(
                with(density) { loupeRadius.toPx() + 16.dp.toPx() },
                maxWidthPx - with(density) { loupeRadius.toPx() + 16.dp.toPx() }
            )

            Box(
                modifier = Modifier
                    .size(loupeDiameter)
                    .align(Alignment.TopStart)
                    .padding(0.dp)
                    .offset {
                        IntOffset(
                            (loupeOffsetX - with(density) { loupeRadius.toPx() }).roundToInt(),
                            (loupeOffsetY - with(density) { loupeRadius.toPx() }).roundToInt()
                        )
                    }
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val zoom = 2.4f
                    // Calculate pixel coordinate in original bitmap
                    val bmpX = (targetCorner.x - imageBounds.left) / scaleFactor
                    val bmpY = (targetCorner.y - imageBounds.top) / scaleFactor

                    val loupeCenter = Offset(size.width / 2f, size.height / 2f)

                    // Draw magnified region
                    val srcW = (size.width / zoom) / scaleFactor
                    val srcH = (size.height / zoom) / scaleFactor

                    val srcLeft = (bmpX - srcW / 2f).coerceAtLeast(0f)
                    val srcTop = (bmpY - srcH / 2f).coerceAtLeast(0f)

                    val dstLeft = loupeCenter.x - (bmpX - srcLeft) * scaleFactor * zoom
                    val dstTop = loupeCenter.y - (bmpY - srcTop) * scaleFactor * zoom

                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
                        dstSize = IntSize(
                            (bitmap.width * scaleFactor * zoom).roundToInt(),
                            (bitmap.height * scaleFactor * zoom).roundToInt()
                        )
                    )

                    // Draw Crosshair Reticle at center
                    drawLine(
                        color = Color.Red,
                        start = Offset(loupeCenter.x - 14.dp.toPx(), loupeCenter.y),
                        end = Offset(loupeCenter.x + 14.dp.toPx(), loupeCenter.y),
                        strokeWidth = 1.8.dp.toPx()
                    )
                    drawLine(
                        color = Color.Red,
                        start = Offset(loupeCenter.x, loupeCenter.y - 14.dp.toPx()),
                        end = Offset(loupeCenter.x, loupeCenter.y + 14.dp.toPx()),
                        strokeWidth = 1.8.dp.toPx()
                    )
                    // Border ring
                    drawCircle(
                        color = Color.White,
                        radius = size.width / 2f - 1.5.dp.toPx(),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        // Bottom Toolbar (Reset, Auto-Fit, Apply)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Reset to full image bounds
                IconButton(
                    onClick = {
                        haptics.select()
                        if (imageBounds.width > 0) {
                            quadPoints[0] = Offset(imageBounds.left, imageBounds.top)
                            quadPoints[1] = Offset(imageBounds.right, imageBounds.top)
                            quadPoints[2] = Offset(imageBounds.right, imageBounds.bottom)
                            quadPoints[3] = Offset(imageBounds.left, imageBounds.bottom)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x28FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Full Image Frame",
                        tint = Color.White
                    )
                }

                // Smart Document Edge Auto-Snap
                IconButton(
                    onClick = {
                        haptics.confirm()
                        val autoQuad = DocumentEdgeDetector.detectDocumentQuad(bitmap)
                        if (autoQuad != null && scaleFactor > 0f) {
                            quadPoints[0] = Offset(imageBounds.left + autoQuad.topLeft.x * scaleFactor, imageBounds.top + autoQuad.topLeft.y * scaleFactor)
                            quadPoints[1] = Offset(imageBounds.left + autoQuad.topRight.x * scaleFactor, imageBounds.top + autoQuad.topRight.y * scaleFactor)
                            quadPoints[2] = Offset(imageBounds.left + autoQuad.bottomRight.x * scaleFactor, imageBounds.top + autoQuad.bottomRight.y * scaleFactor)
                            quadPoints[3] = Offset(imageBounds.left + autoQuad.bottomLeft.x * scaleFactor, imageBounds.top + autoQuad.bottomLeft.y * scaleFactor)
                        } else if (imageBounds.width > 0) {
                            val insetX = imageBounds.width * 0.05f
                            val insetY = imageBounds.height * 0.05f
                            quadPoints[0] = Offset(imageBounds.left + insetX, imageBounds.top + insetY)
                            quadPoints[1] = Offset(imageBounds.right - insetX, imageBounds.top + insetY)
                            quadPoints[2] = Offset(imageBounds.right - insetX, imageBounds.bottom - insetY)
                            quadPoints[3] = Offset(imageBounds.left + insetX, imageBounds.bottom - insetY)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x28FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Smart Edge Auto-Detect",
                        tint = Color(0xFF69F0AE)
                    )
                }
            }

            // Apply Deskew & Crop
            Button(
                onClick = {
                    if (scaleFactor > 0f) {
                        // Map 4 screen points to original bitmap coordinates
                        val pointsInBitmap = quadPoints.map { pt ->
                            Offset(
                                x = ((pt.x - imageBounds.left) / scaleFactor).coerceIn(0f, bitmap.width.toFloat()),
                                y = ((pt.y - imageBounds.top) / scaleFactor).coerceIn(0f, bitmap.height.toFloat())
                            )
                        }
                        val warped = FreeCrop.crop(bitmap, pointsInBitmap)
                        onCropped(warped)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Apply Crop",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Apply Deskew",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
