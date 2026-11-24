package com.abhisheksati.mydoc.ui.files

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

internal data class QuickPdfPage(
    val index: Int = 0,
    val count: Int = 0,
    val bitmap: Bitmap? = null,
    val locked: Boolean = false
)

/** A single bounded page; no document or native handle survives this call. */
internal suspend fun loadQuickPdfPage(file: File, requestedPage: Int): QuickPdfPage = withContext(Dispatchers.IO) {
    var renderer: MuPdfRenderer? = null
    var bitmap: Bitmap? = null
    try {
        currentCoroutineContext().ensureActive()
        renderer = MuPdfRenderer(filePath = file.absolutePath)
        if (renderer.isPasswordProtected) return@withContext QuickPdfPage(locked = true)
        val count = renderer.pageCount
        if (count <= 0) return@withContext QuickPdfPage()
        val index = requestedPage.coerceIn(0, count - 1)
        val (width, height) = renderer.getPageSize(index)
        if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return@withContext QuickPdfPage()
        bitmap = renderer.renderPage(index, 1600f / maxOf(width, height), ReadingMode.NORMAL)
        currentCoroutineContext().ensureActive()
        QuickPdfPage(index, count, bitmap)
    } catch (cancelled: CancellationException) {
        bitmap?.recycle()
        throw cancelled
    } catch (_: Exception) {
        bitmap?.recycle()
        QuickPdfPage()
    } catch (_: OutOfMemoryError) {
        bitmap?.recycle()
        QuickPdfPage()
    } finally {
        renderer?.close()
    }
}

/**
 * Content stays in the sheet; Open continues into the full reader.
 * Only the current page is retained. Published bitmaps are GC-owned, like the reader tile cache:
 * Compose may still draw a previous frame after replacement/disposal, so eagerly recycling a
 * displayed bitmap here is unsafe. Unpublished cancelled/failed renders are recycled by the loader.
 */
@Composable
internal fun QuickPdfPreview(item: StorageItem, index: Int, onPageChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val identity = thumbnailKey(item.path, item.sizeBytes, item.lastModified)
    var page by remember(identity) { mutableStateOf<QuickPdfPage?>(null) }
    var loading by remember(identity) { mutableStateOf(true) }
    LaunchedEffect(identity, index) {
        loading = true
        page = loadQuickPdfPage(item.file, index)
        loading = false
    }
    val result = page
    val ready = !loading && result != null && result.count > 0
    val pageLabel = if (ready) stringResource(R.string.quick_preview_page, result.index + 1, result.count) else null
    Column(modifier) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(Modifier.size(28.dp))
                result?.bitmap != null -> Image(result.bitmap.asImageBitmap(), contentDescription = pageLabel,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                else -> Text(
                    stringResource(if (result?.locked == true) R.string.quick_preview_locked else R.string.quick_preview_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(20.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
        if (result != null && result.count > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onPageChange(result.index - 1) }, enabled = ready && result.index > 0) {
                    Icon(Icons.AutoMirrored.Outlined.NavigateBefore, stringResource(R.string.quick_preview_previous))
                }
                Text(pageLabel ?: stringResource(R.string.quick_preview_loading),
                    modifier = Modifier.weight(1f, fill = false).semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { onPageChange(result.index + 1) }, enabled = ready && result.index < result.count - 1) {
                    Icon(Icons.AutoMirrored.Outlined.NavigateNext, stringResource(R.string.quick_preview_next))
                }
            }
        }
    }
}
