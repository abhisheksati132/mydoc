package com.abhisheksati.mydoc.tools.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.abhisheksati.mydoc.tools.scan.cropper.FreeCornersCropperDialog
import com.abhisheksati.mydoc.tools.scan.filters.ScanFilter
import com.abhisheksati.mydoc.tools.scan.filters.ScanFilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import java.io.File

enum class ScanAspectMode(val label: String, val ratio: Float) {
    DOCUMENT("Document", 0.71f),
    ID_CARD("ID Card", 0.63f),
    FULL("Full View", 0.85f)
}

@Composable
fun CameraScanSheet(
    onDismiss: () -> Unit,
    onScannedUris: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Wrap entire sheet in a full-screen Dialog to ensure system back gestures dismiss the scanner
    // and never inadvertently close the host activity or leak back stack events.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val colors = LocalAppColors.current
        val typography = LocalAppTypography.current
        val haptic = LocalHapticFeedback.current

        var hasCameraPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Toast.makeText(context, "Camera permission is required to scan documents.", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }

        LaunchedEffect(Unit) {
            if (!hasCameraPermission) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        val capturedFiles = remember { mutableStateListOf<File>() }
        var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
        var cameraControl: CameraControl? by remember { mutableStateOf(null) }
        var isTorchOn by remember { mutableStateOf(false) }
        var selectedMode by remember { mutableStateOf(ScanAspectMode.DOCUMENT) }
        var isCapturing by remember { mutableStateOf(false) }

        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var selectedScanFilter by remember { mutableStateOf(ScanFilter.AUTO) }
        var editingPageForCrop by remember { mutableStateOf<Pair<Int, android.graphics.Bitmap>?>(null) }
        var isApplyingFilterAndSaving by remember { mutableStateOf(false) }

        // Perspective Free-Corners Cropper Dialog
        if (editingPageForCrop != null) {
            FreeCornersCropperDialog(
                bitmap = editingPageForCrop!!.second,
                onDismiss = { editingPageForCrop = null },
                onCropped = { croppedBitmap ->
                    val pageIdx = editingPageForCrop!!.first
                    editingPageForCrop = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val file = capturedFiles[pageIdx]
                            FileOutputStream(file).use { out ->
                                croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 94, out)
                                out.flush()
                            }
                            withContext(Dispatchers.Main) {
                                val f = capturedFiles[pageIdx]
                                capturedFiles.removeAt(pageIdx)
                                capturedFiles.add(pageIdx, f)
                                Toast.makeText(context, "Page ${pageIdx + 1} deskewed & cropped.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                accentColor = colors.accent
            )
        }

        // Gallery picker launcher
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris ->
            if (uris.isNotEmpty()) {
                val allUris = capturedFiles.map { Uri.fromFile(it) } + uris
                onScannedUris(allUris)
            }
        }

        // Animated laser scanline
        val infiniteTransition = rememberInfiniteTransition(label = "scannerLaser")
        val laserProgress by infiniteTransition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laserY"
        )

        if (!hasCameraPermission) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Requesting Camera Permission...", style = typography.body, color = Color.White)
            }
            return@Dialog
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // CameraX Live Preview View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Dynamic Viewfinder with Scrim Mask & Laser Guide
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val screenW = maxWidth
                val screenH = maxHeight

                // Calculate viewfinder frame size based on aspect ratio
                val targetAspect = selectedMode.ratio
                val maxFrameWidth = screenW * 0.88f
                val maxFrameHeight = screenH * 0.58f

                val (frameWidth, frameHeight) = if (maxFrameWidth / maxFrameHeight > targetAspect) {
                    Pair(maxFrameHeight * targetAspect, maxFrameHeight)
                } else {
                    Pair(maxFrameWidth, maxFrameWidth / targetAspect)
                }

                // Semi-transparent Scrim Mask with punched-out frame
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameLeft = (size.width - frameWidth.toPx()) / 2f
                    val frameTop = (size.height - frameHeight.toPx()) / 2f - 24.dp.toPx()
                    val frameRight = frameLeft + frameWidth.toPx()
                    val frameBottom = frameTop + frameHeight.toPx()
                    val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())

                    // Path covering full screen with cutout
                    val scrimPath = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                        addRoundRect(
                            RoundRect(
                                left = frameLeft,
                                top = frameTop,
                                right = frameRight,
                                bottom = frameBottom,
                                cornerRadius = cornerRadius
                            )
                        )
                    }
                    drawPath(scrimPath, color = Color(0x99000000))

                    // Document Corner Guides
                    val stroke = 3.5.dp.toPx()
                    val cornerLen = 30.dp.toPx()
                    val cornerColor = Color(0xFF00E676) // High-contrast scanner green/accent

                    // Top-Left corner
                    drawLine(cornerColor, Offset(frameLeft, frameTop + cornerLen), Offset(frameLeft, frameTop), stroke, cap = StrokeCap.Round)
                    drawLine(cornerColor, Offset(frameLeft, frameTop), Offset(frameLeft + cornerLen, frameTop), stroke, cap = StrokeCap.Round)

                    // Top-Right corner
                    drawLine(cornerColor, Offset(frameRight - cornerLen, frameTop), Offset(frameRight, frameTop), stroke, cap = StrokeCap.Round)
                    drawLine(cornerColor, Offset(frameRight, frameTop), Offset(frameRight, frameTop + cornerLen), stroke, cap = StrokeCap.Round)

                    // Bottom-Left corner
                    drawLine(cornerColor, Offset(frameLeft, frameBottom - cornerLen), Offset(frameLeft, frameBottom), stroke, cap = StrokeCap.Round)
                    drawLine(cornerColor, Offset(frameLeft, frameBottom), Offset(frameLeft + cornerLen, frameBottom), stroke, cap = StrokeCap.Round)

                    // Bottom-Right corner
                    drawLine(cornerColor, Offset(frameRight - cornerLen, frameBottom), Offset(frameRight, frameBottom), stroke, cap = StrokeCap.Round)
                    drawLine(cornerColor, Offset(frameRight, frameBottom), Offset(frameRight, frameBottom - cornerLen), stroke, cap = StrokeCap.Round)

                    // Animated Laser Guide
                    val laserY = frameTop + (frameBottom - frameTop) * laserProgress
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xCC00E676),
                                Color(0xFF69F0AE),
                                Color(0xCC00E676),
                                Color.Transparent
                            )
                        ),
                        start = Offset(frameLeft + 4.dp.toPx(), laserY),
                        end = Offset(frameRight - 4.dp.toPx(), laserY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Helper Hint text below frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = frameHeight + 16.dp)
                    ) {
                        Text(
                            text = "Align document inside frame",
                            style = typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Overlay Interface: Top Header Bar & Mode Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Top control bar
                Surface(
                    color = Color(0xB3121212),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(0.7.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close Scanner", tint = Color.White)
                        }

                        // Scanned Page Count Pill
                        Surface(
                            color = if (capturedFiles.isEmpty()) Color(0x33FFFFFF) else colors.accent.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (capturedFiles.isEmpty()) "0 pages scanned" else "${capturedFiles.size} ${if (capturedFiles.size == 1) "page" else "pages"}",
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (capturedFiles.isEmpty()) Color.White else colors.accent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Flashlight / Torch Toggle
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isTorchOn = !isTorchOn
                                    cameraControl?.enableTorch(isTorchOn)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                    contentDescription = "Toggle Torch",
                                    tint = if (isTorchOn) Color(0xFFFFD54F) else Color.White
                                )
                            }

                            if (capturedFiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val uris = capturedFiles.map { Uri.fromFile(it) }
                                        onScannedUris(uris)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Done (${capturedFiles.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Aspect Mode Switcher Chips (Document, ID Card, Full)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = Color(0x991E1E1E),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.6.dp, Color(0x22FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ScanAspectMode.entries.forEach { mode ->
                                val isSelected = selectedMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0x40FFFFFF) else Color.Transparent)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedMode = mode
                                        }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = typography.caption.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White else Color(0xAAFFFFFF)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section: Multi-page Thumbnails Carousel & Shutter Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Scanned Pages Horizontal Strip (collapsible if empty)
                AnimatedVisibility(
                    visible = capturedFiles.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        itemsIndexed(
                            items = capturedFiles,
                            key = { _, file -> file.absolutePath }
                        ) { idx, file ->
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, colors.accent, RoundedCornerShape(8.dp))
                                    .background(Color(0xFF212121))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                                                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                                                var sampleSize = 1
                                                while (maxDim / sampleSize > 2048) { sampleSize *= 2 }
                                                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                                val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                                                if (bmp != null) {
                                                    withContext(Dispatchers.Main) {
                                                        editingPageForCrop = idx to bmp
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(file)
                                        .size(160, 215)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Page ${idx + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Page Number badge
                                Surface(
                                    color = Color(0xCC000000),
                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = typography.caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                // Delete thumbnail button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCCFF5252))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            capturedFiles.removeAt(idx)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove page",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Scan Filter Presets Strip
                AnimatedVisibility(
                    visible = capturedFiles.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScanFilter.entries.forEach { filter ->
                            val isSelected = selectedScanFilter == filter
                            Surface(
                                color = if (isSelected) colors.accent else Color(0x6618181C),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (isSelected) colors.accent else Color(0x33FFFFFF)),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedScanFilter = filter
                                }
                            ) {
                                Text(
                                    text = filter.label,
                                    style = typography.caption.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                    color = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Shutter Controls Container
                Surface(
                    color = Color(0xCC0D0D0D),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    border = BorderStroke(0.6.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Import button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "Import from gallery",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Premium Shutter Trigger
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .border(3.5.dp, Color.White, CircleShape)
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isCapturing) colors.accent else Color.White)
                                    .clickable(enabled = !isCapturing) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val capture = imageCapture ?: return@clickable
                                        isCapturing = true

                                        val tempFile = File.createTempFile(
                                            "scan_page_${capturedFiles.size + 1}_",
                                            ".jpg",
                                            context.cacheDir
                                        )
                                        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                                        capture.takePicture(
                                            outputOptions,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    isCapturing = false
                                                    capturedFiles.add(tempFile)
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    isCapturing = false
                                                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                            )
                        }

                        // Right slot: Finalize Checkmark or spacer
                        Box(
                            modifier = Modifier.size(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (capturedFiles.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (selectedScanFilter == ScanFilter.ORIGINAL) {
                                            val uris = capturedFiles.map { Uri.fromFile(it) }
                                            onScannedUris(uris)
                                        } else {
                                            isApplyingFilterAndSaving = true
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    for (file in capturedFiles) {
                                                        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                                                        val filtered = ScanFilterEngine.applyFilter(bmp, selectedScanFilter)
                                                        FileOutputStream(file).use { out ->
                                                            filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
                                                            out.flush()
                                                        }
                                                        if (filtered != bmp) {
                                                            filtered.recycle()
                                                        }
                                                        bmp.recycle()
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        isApplyingFilterAndSaving = false
                                                        val uris = capturedFiles.map { Uri.fromFile(it) }
                                                        onScannedUris(uris)
                                                    }
                                                } catch (_: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        isApplyingFilterAndSaving = false
                                                        val uris = capturedFiles.map { Uri.fromFile(it) }
                                                        onScannedUris(uris)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isApplyingFilterAndSaving,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(if (isApplyingFilterAndSaving) Color.Gray else colors.accent)
                                ) {
                                    if (isApplyingFilterAndSaving) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Done Scanning",
                                            tint = Color.Black,
                                            modifier = Modifier.size(26.dp)
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
