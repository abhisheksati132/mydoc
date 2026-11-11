package com.abhisheksati.mydoc.tools.ui.components

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.designsystem.tokens.rememberGatedHaptics
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
        val haptic = rememberGatedHaptics()
        val haptics = rememberAppHaptics()

        val pages = remember { mutableStateListOf<OrganizedPageItem>() }
        val thumbnailCache = remember { mutableStateMapOf<Int, Bitmap>() }
        val selectedIds = remember { mutableStateListOf<String>() }
        val thumbnailMutex = remember { Mutex() }
        var sourceDocument by remember { mutableStateOf<Document?>(null) }
        var sourceTempFile by remember { mutableStateOf<File?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                val docToDestroy = sourceDocument
                val tempToDelete = sourceTempFile
                val bitmaps = thumbnailCache.values.toList()
                sourceDocument = null
                sourceTempFile = null
                thumbnailCache.clear()
                bitmaps.forEach { runCatching { if (!it.isRecycled) it.recycle() } }
                // Safely destroy native document under thumbnailMutex off the Main thread
                // so no in-flight thumbnail render touches a destroyed native pointer.
                java.util.concurrent.ForkJoinPool.commonPool().execute {
                    runBlocking {
                        thumbnailMutex.withLock {
                            try { docToDestroy?.destroy() } catch (_: Exception) {}
                            try { tempToDelete?.delete() } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Open the document once and keep it for the sheet's lifetime: thumbnails are rendered as
        // their tiles scroll into view. Rendering every page up front and keeping only the last 35
        // left the other tiles blank forever once they had been evicted.
        LaunchedEffect(pdfUri) {
            isLoading = true
            errorMessage = null
            withContext(Dispatchers.IO) {
                var tempFile: File? = null
                var doc: Document? = null
                var handedOff = false
                try {
                    tempFile = File.createTempFile("organizer_preview_", ".pdf", context.cacheDir)
                    val stream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, pdfUri)
                        ?: context.contentResolver.openInputStream(pdfUri)

                    stream?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }

                    doc = Document.openDocument(tempFile.absolutePath)
                    val total = doc.countPages()

                    withContext(Dispatchers.Main) {
                        pages.clear()
                        for (p in 0 until total) {
                            pages.add(OrganizedPageItem(originalPageIndex = p))
                        }
                        sourceDocument = doc
                        sourceTempFile = tempFile
                        handedOff = true
                        isLoading = false
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = e.localizedMessage ?: "Failed to open document pages"
                    }
                } finally {
                    if (!handedOff) {
                        try { doc?.destroy() } catch (_: Exception) {}
                        try { tempFile?.delete() } catch (_: Exception) {}
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
                        contentDescription = stringResource(R.string.cd_back),
                        tint = scheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.page_organizer),
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
                            contentDescription = stringResource(R.string.cd_toggle_select_all),
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
                                contentDescription = stringResource(R.string.cd_delete_selected),
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
                        AppProgressIndicator(size = 36.dp, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.reorder_loading),
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
                            text = stringResource(R.string.reorder_empty),
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

                            LaunchedEffect(item.originalPageIndex, sourceDocument) {
                                val doc = sourceDocument ?: return@LaunchedEffect
                                renderPageThumbnail(doc, item.originalPageIndex, thumbnailMutex, thumbnailCache)
                            }

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
                                            contentDescription = stringResource(R.string.cd_select),
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
                                                contentDescription = stringResource(R.string.page_label, index + 1),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            AppProgressIndicator(size = 20.dp)
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
                                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                                contentDescription = stringResource(R.string.cd_move_left),
                                                tint = if (index > 0) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.38f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                haptics.tick()
                                                pages.removeAt(index)
                                                selectedIds.remove(item.uniqueId)
                                            },
                                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = stringResource(R.string.cd_remove_page),
                                                tint = scheme.error,
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
                                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                contentDescription = stringResource(R.string.cd_move_right),
                                                tint = if (index < pages.size - 1) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.38f),
                                                modifier = Modifier.size(20.dp)
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
                    text = stringResource(R.string.reorder_pages_organized, pages.size),
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
                    Text(stringResource(R.string.action_apply_order))
                }
            }
        }
    }
}

/**
 * Renders one 240 px RGB_565 thumbnail, serialised by [mutex] because the native document is not
 * safe to touch from two threads. The cache keeps 48 pages; anything evicted is simply drawn again
 * the next time its tile scrolls back into view.
 */
private suspend fun renderPageThumbnail(
    doc: Document,
    index: Int,
    mutex: Mutex,
    cache: SnapshotStateMap<Int, Bitmap>
) {
    mutex.withLock {
        if (cache.containsKey(index)) return
        val bitmap = try {
            withContext(Dispatchers.IO) {
                val page = doc.loadPage(index)
                try {
                    val bounds = page.bounds
                    val scale = 240f / maxOf(bounds.x1 - bounds.x0, 100f)
                    val pixmap = page.toPixmap(Matrix(scale, scale), ColorSpace.DeviceRGB, true)
                    try {
                        val bmp = Bitmap.createBitmap(pixmap.width, pixmap.height, Bitmap.Config.RGB_565)
                        bmp.setPixels(pixmap.pixels, 0, pixmap.width, 0, 0, pixmap.width, pixmap.height)
                        bmp
                    } finally {
                        pixmap.destroy()
                    }
                } finally {
                    page.destroy()
                }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            android.util.Log.w("ReorderPages", "Thumbnail render failed for page $index", t)
            return
        }
        while (cache.size >= 48) {
            val evictKey = cache.keys.maxByOrNull { kotlin.math.abs(it - index) } ?: break
            cache.remove(evictKey) // Let GC reclaim safely; do not recycle in case Compose is rendering it
        }
        cache[index] = bitmap
    }
}
