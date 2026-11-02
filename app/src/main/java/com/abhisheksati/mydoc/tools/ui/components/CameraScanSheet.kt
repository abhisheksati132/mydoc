package com.abhisheksati.mydoc.tools.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.abhisheksati.mydoc.designsystem.animation.pressScale
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.CaptureChromeColors
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures
import com.abhisheksati.mydoc.tools.scan.cropper.FreeCornersCropperDialog
import com.abhisheksati.mydoc.tools.scan.filters.ScanFilter
import com.abhisheksati.mydoc.tools.scan.filters.ScanFilterEngine
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        OverlaySystemBars()
        BackHandler(onBack = onDismiss)

        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                // Compose sizes this dialog against the display while the window's decor sits
                // inside the bars. Darken everything behind it so the bar bands read as part of
                // the viewfinder instead of showing the host screen.
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(1f)
            }
            onDispose {}
        }

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val haptics = rememberAppHaptics()
        val haptic = LocalHapticFeedback.current
        val density = androidx.compose.ui.platform.LocalDensity.current

        // This dialog's window hands Compose zero insets (the platform dialog window is fitted
        // separately from the full-bleed view Compose lays out), so bars padding reports 0 and
        // the shutter bar ended up underneath the navigation bar. Measure the safe area against
        // the window instead: it is correct whether the decor is fitted or full screen.
        var safeTopPx by remember { mutableIntStateOf(0) }
        var safeBottomPx by remember { mutableIntStateOf(0) }
        // Measured chrome heights, so the viewfinder is centred in the camera window rather
        // than under the page head and shutter band.
        var topChromePx by remember { mutableIntStateOf(0) }
        var bottomChromePx by remember { mutableIntStateOf(0) }

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
        val shutterInteraction = remember { MutableInteractionSource() }
        val commitInteraction = remember { MutableInteractionSource() }
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
                accentColor = MaterialTheme.colorScheme.primary
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

        if (!hasCameraPermission) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Requesting Camera Permission...", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            return@Dialog
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .onGloballyPositioned { c ->
                    val frame = android.graphics.Rect()
                    view.getWindowVisibleDisplayFrame(frame)
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    safeTopPx = (frame.top - location[1]).coerceAtLeast(0)
                    safeBottomPx = (location[1] + c.size.height - frame.bottom).coerceAtLeast(0)
                }
                .padding(
                    top = with(density) { safeTopPx.toDp() },
                    bottom = with(density) { safeBottomPx.toDp() }
                )
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

                // Calculate viewfinder frame size based on aspect ratio with generous clearance
                val targetAspect = selectedMode.ratio
                val maxFrameWidth = screenW * 0.86f
                val maxFrameHeight = screenH * 0.46f

                val (frameWidth, frameHeight) = if (maxFrameWidth / maxFrameHeight > targetAspect) {
                    Pair(maxFrameHeight * targetAspect, maxFrameHeight)
                } else {
                    Pair(maxFrameWidth, maxFrameWidth / targetAspect)
                }

                val chromeTop = with(density) { topChromePx.toDp() }
                val chromeBottom = with(density) { bottomChromePx.toDp() }

                // Semi-transparent Scrim Mask with punched-out frame
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameLeft = (size.width - frameWidth.toPx()) / 2f
                    // Centre the frame in the camera window between the two chrome bands.
                    val windowTop = chromeTop.toPx()
                    val windowBottom = size.height - chromeBottom.toPx()
                    val frameTop = windowTop + (windowBottom - windowTop - frameHeight.toPx()) / 2f
                    val frameRight = frameLeft + frameWidth.toPx()
                    val frameBottom = frameTop + frameHeight.toPx()
                    val cornerRadius = CornerRadius(with(density) { 4.dp.toPx() }, with(density) { 4.dp.toPx() })

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
                    drawPath(scrimPath, color = CaptureChromeColors.scrim)

                    // Document Corner Guides
                    val stroke = AppSpacing.rule.toPx()
                    val cornerLen = 24.dp.toPx()
                    val cornerColor = Color.White.copy(alpha = 0.90f)

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
                }
            }

            // Page head: kicker, title, page-count pill, and the spectrum rule. Mode
            // selection is a gradient-underlined tab row, matching the reader's sheet tabs.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CaptureChromeColors.sheetScrim)
                    .onGloballyPositioned { topChromePx = it.size.height }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close scanner",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text(
                            text = "SCANNER",
                            style = MaterialTheme.typography.labelSmall,
                            color = CaptureChromeColors.onChromeMuted
                        )
                        Text(
                            text = "Scan document",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    if (capturedFiles.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${capturedFiles.size} ${if (capturedFiles.size == 1) "PAGE" else "PAGES"}",
                                style = MaterialTheme.typography.labelSmall.withTabularFigures(),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = "Toggle torch",
                            tint = if (isTorchOn) CaptureChromeColors.torchOn else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Tabs sit on the title's axis; the close glyph hangs in the margin.
                        .padding(start = 52.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    ScanAspectMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .clickable { selectedMode = mode }
                                .padding(top = 8.dp, bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else CaptureChromeColors.onChromeMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            )
                        }
                    }
                }

                Text(
                    text = "Align the page inside the frame",
                    style = MaterialTheme.typography.labelMedium,
                    color = CaptureChromeColors.onChromeMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 10.dp)
                )
            }

            // Bottom band: contact sheet, filter tabs and shutter, closed by the same
            // spectrum rule as the page head so the two chrome bands read as one system.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(CaptureChromeColors.sheetScrim)
                    .onGloballyPositioned { bottomChromePx = it.size.height }
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Scanned Pages Horizontal Strip (collapsible if empty)
                AnimatedVisibility(
                    visible = capturedFiles.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = capturedFiles,
                            key = { _, file -> file.absolutePath }
                        ) { idx, file ->
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 62.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .border(
                                        AppSpacing.hairline,
                                        CaptureChromeColors.onChromeMuted,
                                        MaterialTheme.shapes.extraSmall
                                    )
                                    .background(CaptureChromeColors.surface)
                                    .clickable {
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

                                // Folio: page numbers are set in tabular figures, never in a pill.
                                Text(
                                    text = (idx + 1).toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.labelMedium.withTabularFigures(),
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(CaptureChromeColors.scrimStrong)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )

                                // Delete thumbnail button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(CaptureChromeColors.scrimStrong)
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

                // Scan Filter Presets Strip: secondary tabs, thinner rule than the mode row.
                AnimatedVisibility(
                    visible = capturedFiles.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScrollWithFade(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ScanFilter.entries.forEach { filter ->
                            val isSelected = selectedScanFilter == filter
                            Column(
                                modifier = Modifier
                                    .width(IntrinsicSize.Max)
                                    .clickable { selectedScanFilter = filter }
                                    .padding(bottom = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = filter.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else CaptureChromeColors.onChromeMuted
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(AppSpacing.rule)
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Import button
                    IconButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = "Import from gallery",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Tactile shutter trigger
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(AppSpacing.rule, Color.White, CircleShape)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isCapturing) MaterialTheme.colorScheme.primary else Color.White)
                                .clickable(
                                    interactionSource = shutterInteraction,
                                    indication = ripple(),
                                    enabled = !isCapturing
                                ) {
                                    haptics.confirm()
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
                                .pressScale(shutterInteraction)
                        )
                    }

                    // Right slot: commit the scan
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedFiles.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(
                                        if (isApplyingFilterAndSaving) {
                                            CaptureChromeColors.panel
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    .clickable(
                                        interactionSource = commitInteraction,
                                        indication = ripple(),
                                        enabled = !isApplyingFilterAndSaving
                                    ) {
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
                                    }
                                    .pressScale(commitInteraction),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isApplyingFilterAndSaving) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Done scanning",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
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
