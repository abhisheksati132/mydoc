package com.abhisheksati.mydoc.tools.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class OrganizedPageItem(
    val uniqueId: String = UUID.randomUUID().toString(),
    val originalPageIndex: Int,
    var rotationDegrees: Int = 0
)

@Composable
fun ReorderPagesSheet(
    pdfUri: Uri,
    onDismiss: () -> Unit,
    onApplyOrder: (List<Int>) -> Unit,
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

        val context = LocalContext.current
        val colors = LocalAppColors.current
        val typography = LocalAppTypography.current
        val haptic = LocalHapticFeedback.current

        val pages = remember { mutableStateListOf<OrganizedPageItem>() }
        val thumbnailCache = remember { mutableMapOf<Int, Bitmap>() }
        val selectedIds = remember { mutableStateListOf<String>() }

        var isLoading by remember { mutableStateOf(true) }
        var isRenderingThumbnails by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Render page thumbnails
        LaunchedEffect(pdfUri) {
            isLoading = true
            errorMessage = null
            withContext(Dispatchers.IO) {
                try {
                    val tempFile = File.createTempFile("organizer_preview_", ".pdf", context.cacheDir)
                    val stream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, pdfUri)
                        ?: context.contentResolver.openInputStream(pdfUri)

                    stream?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }

                    val doc = Document.openDocument(tempFile.absolutePath)
                    val total = doc.countPages()

                    withContext(Dispatchers.Main) {
                        pages.clear()
                        for (p in 0 until total) {
                            pages.add(OrganizedPageItem(originalPageIndex = p))
                        }
                        isLoading = false
                        isRenderingThumbnails = true
                    }

                    // Render thumbnails sequentially to preserve memory
                    for (p in 0 until total) {
                        try {
                            val page = doc.loadPage(p)
                            val bounds = page.bounds
                            val targetWidth = 240f
                            val scale = targetWidth / maxOf(bounds.x1 - bounds.x0, 100f)
                            val matrix = Matrix(scale, scale)
                            val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, false)
                            val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                            pixmap.destroy()
                            page.destroy()

                            withContext(Dispatchers.Main) {
                                thumbnailCache[p] = bmp
                            }
                        } catch (_: Throwable) {}
                    }

                    doc.destroy()
                    tempFile.delete()

                    withContext(Dispatchers.Main) {
                        isRenderingThumbnails = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = e.localizedMessage ?: "Failed to open document pages"
                    }
                }
            }
        }

        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Action Bar
                Surface(
                    color = colors.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Page Organizer",
                                    style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (selectedIds.isEmpty()) "${pages.size} pages" else "${selectedIds.size} of ${pages.size} selected",
                                    style = typography.caption,
                                    color = if (selectedIds.isEmpty()) colors.textSecondary else colors.accent
                                )
                            }
                        }

                        // Batch Action Icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Select / Deselect All
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (selectedIds.size == pages.size) {
                                        selectedIds.clear()
                                    } else {
                                        selectedIds.clear()
                                        selectedIds.addAll(pages.map { it.uniqueId })
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedIds.size == pages.size && pages.isNotEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.SelectAll,
                                    contentDescription = "Toggle Select All",
                                    tint = colors.textPrimary
                                )
                            }

                            // Rotate Selected
                            if (selectedIds.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        pages.forEachIndexed { idx, item ->
                                            if (item.uniqueId in selectedIds) {
                                                pages[idx] = item.copy(rotationDegrees = (item.rotationDegrees + 90) % 360)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                                        contentDescription = "Rotate Selected",
                                        tint = colors.accent
                                    )
                                }

                                // Delete Selected
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        pages.removeAll { it.uniqueId in selectedIds }
                                        selectedIds.clear()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }
                    }
                }

                // Grid Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Loading document pages...", style = typography.body, color = colors.textSecondary)
                        }
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = errorMessage!!, style = typography.body, color = Color(0xFFFF5252))
                        }
                    } else if (pages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "No pages remaining. Add or restore pages.", style = typography.body, color = colors.textSecondary)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 108.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(pages, key = { _, item -> item.uniqueId }) { index, item ->
                                val bmp = thumbnailCache[item.originalPageIndex]
                                val isSelected = item.uniqueId in selectedIds
                                val rotation by animateFloatAsState(targetValue = item.rotationDegrees.toFloat(), label = "pageRot")

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.8.dp,
                                            color = if (isSelected) colors.accent else colors.divider,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (isSelected) selectedIds.remove(item.uniqueId)
                                            else selectedIds.add(item.uniqueId)
                                        },
                                    color = colors.surfaceVariant,
                                    shadowElevation = if (isSelected) 4.dp else 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Header inside page card: Page index + selection checkbox
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = colors.background,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                    color = colors.textPrimary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }

                                            Icon(
                                                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = "Select",
                                                tint = if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Thumbnail image with animated rotation
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.72f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Page ${index + 1}",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer { rotationZ = rotation }
                                                )
                                            } else {
                                                CircularProgressIndicator(
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(20.dp),
                                                    color = colors.accent
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Card Controls: Move Left, Rotate, Move Right, Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Move left
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (index > 0) {
                                                        val temp = pages[index]
                                                        pages[index] = pages[index - 1]
                                                        pages[index - 1] = temp
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                                    contentDescription = "Move left",
                                                    tint = if (index > 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Rotate 90
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    pages[index] = item.copy(rotationDegrees = (item.rotationDegrees + 90) % 360)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                                                    contentDescription = "Rotate 90°",
                                                    tint = colors.accent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            // Move right
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (index < pages.size - 1) {
                                                        val temp = pages[index]
                                                        pages[index] = pages[index + 1]
                                                        pages[index + 1] = temp
                                                    }
                                                },
                                                enabled = index < pages.size - 1,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                    contentDescription = "Move right",
                                                    tint = if (index < pages.size - 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Delete page
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    pages.removeAt(index)
                                                    selectedIds.remove(item.uniqueId)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Remove page",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Save Action Bar
                Surface(
                    color = colors.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pages.size} pages organized",
                            style = typography.caption.copy(fontWeight = FontWeight.Medium),
                            color = colors.textSecondary
                        )

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val finalOrder = pages.map { it.originalPageIndex }
                                onApplyOrder(finalOrder)
                            },
                            enabled = pages.isNotEmpty() && !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply & Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
