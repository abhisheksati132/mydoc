package com.abhisheksati.mydoc.tools.ui.components

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class OrganizedPageItem(
    val uniqueId: String = UUID.randomUUID().toString(),
    val originalPageIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderPagesSheet(
    pdfUri: Uri,
    onDismiss: () -> Unit,
    onApplyOrder: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
        val scheme = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography
        val haptic = LocalHapticFeedback.current
        val haptics = rememberAppHaptics()

        val pages = remember { mutableStateListOf<OrganizedPageItem>() }
        val thumbnailCache = remember { mutableStateMapOf<Int, Bitmap>() }
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
                            val pixmap = page.toPixmap(matrix, ColorSpace.DeviceRGB, true)
                            val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                            pixmap.destroy()
                            page.destroy()

                            withContext(Dispatchers.Main) {
                                thumbnailCache[p] = bmp
                            }
                        } catch (t: Throwable) {
                            android.util.Log.w("ReorderPages", "Thumbnail render failed for page $p", t)
                        }
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

    AppBottomSheet(
        isOpen = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = scheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Page Organizer",
                        style = typography.titleLarge,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectedIds.isEmpty()) "${pages.size} pages" else "${selectedIds.size} of ${pages.size} selected",
                        style = typography.bodySmall,
                        color = if (selectedIds.isEmpty()) scheme.onSurfaceVariant else scheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
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
                            tint = if (selectedIds.size == pages.size && pages.isNotEmpty()) scheme.primary else scheme.onSurfaceVariant
                        )
                    }

                    if (selectedIds.isNotEmpty()) {
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
                                tint = scheme.error
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = scheme.outlineVariant)

            // Grid Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading document pages...",
                            style = typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                } else if (errorMessage != null) {
                    val msg = errorMessage ?: ""
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = msg,
                            style = typography.bodyMedium,
                            color = scheme.error
                        )
                    }
                } else if (pages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No pages remaining. Add or restore pages.",
                            style = typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 108.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(pages, key = { _, item -> item.uniqueId }) { index, item ->
                            val bmp = thumbnailCache[item.originalPageIndex]
                            val isSelected = item.uniqueId in selectedIds

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppRadius.cardShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) scheme.primary else scheme.outlineVariant,
                                        shape = AppRadius.cardShape
                                    )
                                    .clickable {
                                        if (isSelected) selectedIds.remove(item.uniqueId)
                                        else selectedIds.add(item.uniqueId)
                                    },
                                color = if (isSelected) scheme.surfaceContainerHigh else scheme.surfaceContainerLow,
                                shape = AppRadius.cardShape
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Page index + selection state
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = scheme.surfaceContainerHighest,
                                            shape = AppRadius.microShape
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = typography.labelSmall,
                                                color = scheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Icon(
                                            imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                            contentDescription = "Select",
                                            tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Page render stays on white paper regardless of theme
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.72f)
                                            .clip(AppRadius.microShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Page ${index + 1}",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Card controls: move left, move right, remove
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                haptics.tick()
                                                if (index > 0) {
                                                    val temp = pages[index]
                                                    pages[index] = pages[index - 1]
                                                    pages[index - 1] = temp
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                                contentDescription = "Move left",
                                                tint = if (index > 0) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.38f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                haptics.tick()
                                                if (index < pages.size - 1) {
                                                    val temp = pages[index]
                                                    pages[index] = pages[index + 1]
                                                    pages[index + 1] = temp
                                                }
                                            },
                                            enabled = index < pages.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                contentDescription = "Move right",
                                                tint = if (index < pages.size - 1) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.38f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                pages.removeAt(index)
                                                selectedIds.remove(item.uniqueId)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Remove page",
                                                tint = scheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = scheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pages.size} pages organized",
                    style = typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        haptics.confirm()
                        val finalOrder = pages.map { it.originalPageIndex }
                        onApplyOrder(finalOrder)
                    },
                    enabled = pages.isNotEmpty() && !isLoading,
                    shape = AppRadius.cardShape,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply & Save")
                }
            }
        }
    }
}
