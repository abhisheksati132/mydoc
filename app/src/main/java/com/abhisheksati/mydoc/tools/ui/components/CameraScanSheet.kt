package com.abhisheksati.mydoc.tools.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import java.io.File

@Composable
fun CameraScanSheet(
    onDismiss: () -> Unit,
    onScannedUris: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept back press so it dismisses the scanner instead of quitting the application
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

    if (!hasCameraPermission) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Requesting Camera Permission...", style = typography.body, color = Color.White)
        }
        return
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

        // Overlay Interface
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with Controls in a clean dark pill container
            Surface(
                color = Color(0xB3000000),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.6.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close Scanner", tint = Color.White)
                    }

                    // Scanned page count pill
                    Surface(
                        color = Color(0x40FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (capturedFiles.isEmpty()) "0 pages scanned" else "${capturedFiles.size} ${if (capturedFiles.size == 1) "page" else "pages"} scanned",
                            style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Torch Toggle
                        IconButton(
                            onClick = {
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Viewfinder Document Framing Guide (Centered with corner brackets)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight
                val targetAspect = 0.72f // Standard A4 / Document aspect ratio

                val frameModifier = if (availableWidth / availableHeight > targetAspect) {
                    Modifier
                        .height(availableHeight * 0.92f)
                        .aspectRatio(targetAspect)
                } else {
                    Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(targetAspect)
                }

                Box(
                    modifier = frameModifier,
                    contentAlignment = Alignment.Center
                ) {
                    // Document Corner Guides
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 3.dp.toPx()
                        val corner = 28.dp.toPx()
                        val color = Color.White

                        // Top-Left corner
                        drawLine(color, Offset(0f, corner), Offset(0f, 0f), stroke)
                        drawLine(color, Offset(0f, 0f), Offset(corner, 0f), stroke)

                        // Top-Right corner
                        drawLine(color, Offset(size.width - corner, 0f), Offset(size.width, 0f), stroke)
                        drawLine(color, Offset(size.width, 0f), Offset(size.width, corner), stroke)

                        // Bottom-Left corner
                        drawLine(color, Offset(0f, size.height - corner), Offset(0f, size.height), stroke)
                        drawLine(color, Offset(0f, size.height), Offset(corner, size.height), stroke)

                        // Bottom-Right corner
                        drawLine(color, Offset(size.width - corner, size.height), Offset(size.width, size.height), stroke)
                        drawLine(color, Offset(size.width, size.height - corner), Offset(size.width, size.height), stroke)
                    }

                    // Inner subtle frame border
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    )

                    // Helper alignment badge
                    Surface(
                        color = Color(0x88000000),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Align document within frame",
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Bottom Shutter Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left slot: Thumbnail of last captured page
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedFiles.isNotEmpty()) {
                        val lastFile = capturedFiles.last()
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                        ) {
                            AsyncImage(
                                model = lastFile,
                                contentDescription = "Last captured page",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }

                // Center slot: Premium Shutter Trigger
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.5.dp, Color.White, CircleShape)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val capture = imageCapture ?: return@clickable
                                val tempFile = File.createTempFile("scan_page_${capturedFiles.size + 1}_", ".jpg", context.cacheDir)
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            capturedFiles.add(tempFile)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                    )
                }

                // Right slot: Done checkmark when pages exist
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedFiles.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val uris = capturedFiles.map { Uri.fromFile(it) }
                                onScannedUris(uris)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Done Scanning",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

