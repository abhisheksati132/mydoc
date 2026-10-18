package com.abhisheksati.mydoc.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EditorTool(val title: String, val icon: ImageVector) {
    CROP("Crop", Icons.Default.Crop),
    ROTATE("Transform", Icons.AutoMirrored.Filled.RotateRight),
    ADJUST("Adjust", Icons.Default.Tune),
    FILTERS("Filters", Icons.Default.AutoAwesome)
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    PHOTO_4_3("4:3", 4f / 3f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    PORTRAIT_9_16("9:16", 9f / 16f)
}

enum class FilterPreset(val displayName: String) {
    ORIGINAL("Normal"),
    VIVID("Vivid"),
    MONO("B&W"),
    SEPIA("Sepia"),
    WARM("Warm"),
    COOL("Cool"),
    DRAMA("Drama")
}

@Composable
fun PhotoEditorScreen(
    item: MediaItem,
    onDismiss: () -> Unit,
    onPhotoSaved: (File) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Editor adjustments
    var activeTool by remember { mutableStateOf(EditorTool.ADJUST) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var selectedCropRatio by remember { mutableStateOf(CropAspectRatio.ORIGINAL) }
    var selectedFilter by remember { mutableStateOf(FilterPreset.ORIGINAL) }

    // Sliders: -100 to +100
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var warmth by remember { mutableFloatStateOf(0f) }

    // Load bitmap efficiently in background
    LaunchedEffect(item.uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    // Decode bounds first to ensure safe memory scaling if photo is huge (> 4000px)
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeStream(stream, null, opts)
                    originalBitmap = bmp
                }
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    // Compose ColorMatrix calculation for real-time live preview
    val combinedColorMatrix = remember(brightness, contrast, saturation, warmth, selectedFilter) {
        val cm = android.graphics.ColorMatrix()

        // 1. Base preset filter
        when (selectedFilter) {
            FilterPreset.ORIGINAL -> {}
            FilterPreset.VIVID -> {
                val satMatrix = android.graphics.ColorMatrix().apply { setSaturation(1.4f) }
                val contMatrix = createContrastMatrix(1.2f)
                cm.postConcat(satMatrix)
                cm.postConcat(contMatrix)
            }
            FilterPreset.MONO -> {
                cm.setSaturation(0f)
                val contMatrix = createContrastMatrix(1.15f)
                cm.postConcat(contMatrix)
            }
            FilterPreset.SEPIA -> {
                val sepiaMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f,     0f,     0f,     1f, 0f
                ))
                cm.postConcat(sepiaMatrix)
            }
            FilterPreset.WARM -> {
                val warmMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    1.1f, 0f,   0f,   0f, 15f,
                    0f,   1.0f, 0f,   0f, 5f,
                    0f,   0f,   0.9f, 0f, -10f,
                    0f,   0f,   0f,   1f, 0f
                ))
                cm.postConcat(warmMatrix)
            }
            FilterPreset.COOL -> {
                val coolMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.9f, 0f,   0f,   0f, -10f,
                    0f,   0.95f,0f,   0f, 0f,
                    0f,   0f,   1.15f,0f, 20f,
                    0f,   0f,   0f,   1f, 0f
                ))
                cm.postConcat(coolMatrix)
            }
            FilterPreset.DRAMA -> {
                val dramaMatrix = createContrastMatrix(1.45f)
                val satMatrix = android.graphics.ColorMatrix().apply { setSaturation(0.85f) }
                cm.postConcat(dramaMatrix)
                cm.postConcat(satMatrix)
            }
        }

        // 2. Custom adjustments
        // Saturation (-100 to 100 -> 0.0 to 2.0)
        val satFactor = (saturation / 100f) + 1f
        val customSat = android.graphics.ColorMatrix().apply { setSaturation(satFactor.coerceAtLeast(0f)) }
        cm.postConcat(customSat)

        // Contrast (-100 to 100 -> 0.5 to 1.8)
        val contrastFactor = if (contrast >= 0) 1f + (contrast / 100f) * 0.8f else 1f + (contrast / 100f) * 0.5f
        cm.postConcat(createContrastMatrix(contrastFactor))

        // Brightness (-100 to 100 -> -60 to +60)
        val bShift = (brightness / 100f) * 60f
        val bMatrix = android.graphics.ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, bShift,
            0f, 1f, 0f, 0f, bShift,
            0f, 0f, 1f, 0f, bShift,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(bMatrix)

        // Warmth (-100 to 100)
        val wShift = (warmth / 100f) * 30f
        val wMatrix = android.graphics.ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, wShift,
            0f, 1f, 0f, 0f, wShift * 0.3f,
            0f, 0f, 1f, 0f, -wShift,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(wMatrix)

        ColorMatrix(cm.array)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler { onDismiss() }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Header (Cancel, Reset, Save)
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
                        text = "Photo Editor",
                        style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            brightness = 0f
                            contrast = 0f
                            saturation = 0f
                            warmth = 0f
                            rotationAngle = 0f
                            isFlippedHorizontal = false
                            selectedCropRatio = CropAspectRatio.ORIGINAL
                            selectedFilter = FilterPreset.ORIGINAL
                        }) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset",
                                tint = Color(0xFFA0A0A5)
                            )
                        }

                        Button(
                            onClick = {
                                if (originalBitmap != null && !isSaving) {
                                    isSaving = true
                                    scope.launch {
                                        val savedFile = saveEditedPhoto(
                                            context = context,
                                            sourceBitmap = originalBitmap!!,
                                            rotation = rotationAngle,
                                            isFlippedH = isFlippedHorizontal,
                                            aspectRatio = selectedCropRatio,
                                            colorMatrix = combinedColorMatrix
                                        )
                                        isSaving = false
                                        if (savedFile != null) {
                                            Toast.makeText(context, "Saved to ${savedFile.name}", Toast.LENGTH_SHORT).show()
                                            onPhotoSaved(savedFile)
                                        } else {
                                            Toast.makeText(context, "Failed to save edited photo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Middle Canvas Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF000000)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading || originalBitmap == null) {
                        CircularProgressIndicator(color = colors.accent)
                    } else {
                        val bitmap = originalBitmap!!
                        val cropRatio = selectedCropRatio.ratio
                        val boxModifier = if (cropRatio != null) {
                            Modifier
                                .fillMaxSize(0.92f)
                                .aspectRatio(cropRatio)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                        } else {
                            Modifier.fillMaxSize()
                        }

                        Box(
                            modifier = boxModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Editor Preview",
                                contentScale = if (cropRatio != null) ContentScale.Crop else ContentScale.Fit,
                                colorFilter = ColorFilter.colorMatrix(combinedColorMatrix),
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }
                    }
                }

                // Tool Options Tray
                Surface(
                    color = Color(0xFF0A0A0C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        // Dynamic Controls depending on active tool
                        when (activeTool) {
                            EditorTool.CROP -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CropAspectRatio.entries.forEach { ratio ->
                                        val isSelected = selectedCropRatio == ratio
                                        Surface(
                                            color = if (isSelected) colors.accent else Color(0xFF26262A),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.clickable { selectedCropRatio = ratio }
                                        ) {
                                            Text(
                                                text = ratio.label,
                                                color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            EditorTool.ROTATE -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { rotationAngle = (rotationAngle - 90f + 360f) % 360f }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate -90", tint = Color.White)
                                            Text("-90°", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                    IconButton(onClick = { rotationAngle = (rotationAngle + 90f) % 360f }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate +90", tint = Color.White)
                                            Text("+90°", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                    IconButton(onClick = { isFlippedHorizontal = !isFlippedHorizontal }) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = if (isFlippedHorizontal) colors.accent else Color.White)
                                            Text("Flip", fontSize = 10.sp, color = if (isFlippedHorizontal) colors.accent else Color.White)
                                        }
                                    }
                                }
                            }

                            EditorTool.ADJUST -> {
                                var currentSlider by remember { mutableStateOf("Brightness") }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Brightness", "Contrast", "Saturation", "Warmth").forEach { title ->
                                        val isSelected = currentSlider == title
                                        Surface(
                                            color = if (isSelected) colors.accent.copy(alpha = 0.2f) else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, colors.accent) else null,
                                            modifier = Modifier.clickable { currentSlider = title }
                                        ) {
                                            Text(
                                                text = title,
                                                color = if (isSelected) colors.accent else Color(0xFFA0A0A5),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (currVal, onValChange) = when (currentSlider) {
                                        "Brightness" -> brightness to { v: Float -> brightness = v }
                                        "Contrast" -> contrast to { v: Float -> contrast = v }
                                        "Saturation" -> saturation to { v: Float -> saturation = v }
                                        else -> warmth to { v: Float -> warmth = v }
                                    }

                                    Slider(
                                        value = currVal,
                                        onValueChange = onValChange,
                                        valueRange = -100f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = colors.accent,
                                            activeTrackColor = colors.accent,
                                            inactiveTrackColor = Color(0x33FFFFFF)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${currVal.toInt()}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                            }

                            EditorTool.FILTERS -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FilterPreset.entries.forEach { filter ->
                                        val isSelected = selectedFilter == filter
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { selectedFilter = filter }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (filter) {
                                                            FilterPreset.ORIGINAL -> Color(0xFF555555)
                                                            FilterPreset.VIVID -> Color(0xFFFF5252)
                                                            FilterPreset.MONO -> Color(0xFF888888)
                                                            FilterPreset.SEPIA -> Color(0xFFBCAAA4)
                                                            FilterPreset.WARM -> Color(0xFFFFB74D)
                                                            FilterPreset.COOL -> Color(0xFF64B5F6)
                                                            FilterPreset.DRAMA -> Color(0xFF37474F)
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 0.dp,
                                                        color = if (isSelected) colors.accent else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = filter.displayName,
                                                fontSize = 11.sp,
                                                color = if (isSelected) colors.accent else Color(0xFFA0A0A5),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom Navigation Icons (Crop, Transform, Adjust, Filters)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorTool.entries.forEach { tool ->
                                val isSelected = activeTool == tool
                                IconButton(onClick = { activeTool = tool }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = tool.icon,
                                            contentDescription = tool.title,
                                            tint = if (isSelected) colors.accent else Color(0xFFA0A0A5),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = tool.title,
                                            fontSize = 10.sp,
                                            color = if (isSelected) colors.accent else Color(0xFFA0A0A5),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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

private fun createContrastMatrix(contrast: Float): android.graphics.ColorMatrix {
    val scale = contrast
    val translate = (-0.5f * scale + 0.5f) * 255f
    return android.graphics.ColorMatrix(floatArrayOf(
        scale, 0f,    0f,    0f, translate,
        0f,    scale, 0f,    0f, translate,
        0f,    0f,    scale, 0f, translate,
        0f,    0f,    0f,    1f, 0f
    ))
}

private suspend fun saveEditedPhoto(
    context: Context,
    sourceBitmap: Bitmap,
    rotation: Float,
    isFlippedH: Boolean,
    aspectRatio: CropAspectRatio,
    colorMatrix: ColorMatrix
): File? = withContext(Dispatchers.IO) {
    try {
        // 1. Calculate Crop Bounds if selected
        var workingBitmap = sourceBitmap
        val ratio = aspectRatio.ratio
        if (ratio != null) {
            val srcWidth = sourceBitmap.width.toFloat()
            val srcHeight = sourceBitmap.height.toFloat()
            val currentRatio = srcWidth / srcHeight

            val (cropW, cropH) = if (currentRatio > ratio) {
                // Image is wider than crop target
                val h = srcHeight
                val w = (h * ratio)
                w.toInt() to h.toInt()
            } else {
                // Image is taller than crop target
                val w = srcWidth
                val h = (w / ratio)
                w.toInt() to h.toInt()
            }

            val x = ((srcWidth - cropW) / 2f).toInt().coerceAtLeast(0)
            val y = ((srcHeight - cropH) / 2f).toInt().coerceAtLeast(0)
            workingBitmap = Bitmap.createBitmap(sourceBitmap, x, y, cropW.coerceAtMost(sourceBitmap.width - x), cropH.coerceAtMost(sourceBitmap.height - y))
        }

        // 2. Apply Matrix (Rotation & Flip)
        val matrix = Matrix()
        if (rotation != 0f) {
            matrix.postRotate(rotation)
        }
        if (isFlippedH) {
            matrix.postScale(-1f, 1f)
        }

        val transformedBitmap = if (!matrix.isIdentity) {
            Bitmap.createBitmap(workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, matrix, true)
        } else {
            workingBitmap
        }

        // 3. Render ColorMatrix filters into a final output Bitmap
        val finalBitmap = Bitmap.createBitmap(transformedBitmap.width, transformedBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix.values)
        }
        canvas.drawBitmap(transformedBitmap, 0f, 0f, paint)

        // 4. Save to Pictures/mydoc folder
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val myDocDir = File(picturesDir, "mydoc").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val targetFile = File(myDocDir, "IMG_EDIT_${timeStamp}.jpg")

        FileOutputStream(targetFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
        }

        // 5. Trigger Android MediaScanner so it instantly shows in device gallery
        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("image/jpeg"), null)

        targetFile
    } catch (_: Exception) {
        null
    }
}
