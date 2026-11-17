package com.abhisheksati.mydoc.tools.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.components.horizontalScrollWithFade
import com.abhisheksati.mydoc.designsystem.components.showMessage
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.tools.model.ImagePdfQuality
import com.abhisheksati.mydoc.tools.model.PageMargin
import com.abhisheksati.mydoc.tools.model.PageOrientation
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.scan.cropper.FreeCornersCropperDialog
import com.abhisheksati.mydoc.tools.scan.filters.ScanFilter
import com.abhisheksati.mydoc.tools.scan.filters.ScanFilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

private const val PREVIEW_MAX_DIM = 1600
private const val THUMB_MAX_DIM = 240
private const val CROP_MAX_DIM = 3000
private const val BAKE_MAX_DIM = 3000
private const val MAX_SAMPLE_SIZE = 32
private const val STALE_SESSION_MS = 24L * 60 * 60 * 1000

/**
 * Working copy of one page. [file] always lives in this editor's session directory; adjustments
 * are kept as state and baked into a new file only when the PDF is created, so repeated rotate
 * or filter changes never recompress the pixels.
 */
private data class EditorPage(
    val file: File,
    val rotationDegrees: Int = 0,
    val filter: ScanFilter = ScanFilter.ORIGINAL
) {
    val needsBake: Boolean get() = rotationDegrees != 0 || filter != ScanFilter.ORIGINAL
}

private data class CropRequest(val index: Int, val bitmap: Bitmap)

/**
 * Image → PDF review screen: thumbnail order, per-page crop/rotate/filter, page size, orientation,
 * margins, quality and compression. A selected image lands here instead of converting straight away.
 */
@Composable
fun ImagePdfEditorSheet(
    initialUris: List<Uri>,
    options: ToolOptions,
    onOptionsChange: ((ToolOptions) -> ToolOptions) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val scope = rememberCoroutineScope()

    val sessionDir = remember {
        File(File(context.cacheDir, "image_pdf_editor"), "session_${System.currentTimeMillis()}")
    }
    val pages = remember { mutableStateListOf<EditorPage>() }
    var isImporting by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var cropRequest by remember { mutableStateOf<CropRequest?>(null) }

    val addImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val imported = withContext(Dispatchers.IO) {
                    uris.mapNotNull { copyToSession(context, it, sessionDir) }
                }
                pages.addAll(imported.map { EditorPage(it) })
                if (imported.size < uris.size) {
                    context.showMessage(resources.getString(R.string.imgpdf_import_failed), long = true)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val imported = withContext(Dispatchers.IO) {
            sessionDir.parentFile?.let { root ->
                root.mkdirs()
                // Sessions are scratch space left behind by a dismissed editor; they are not
                // deleted on exit because the engine may still be staging their files.
                val cutoff = System.currentTimeMillis() - STALE_SESSION_MS
                root.listFiles()?.forEach { stale ->
                    if (stale.isDirectory && stale != sessionDir && stale.lastModified() < cutoff) {
                        stale.deleteRecursively()
                    }
                }
            }
            sessionDir.mkdirs()
            initialUris.mapNotNull { copyToSession(context, it, sessionDir) }
        }
        pages.addAll(imported.map { EditorPage(it) })
        isImporting = false
        if (imported.isEmpty()) {
            context.showMessage(resources.getString(R.string.imgpdf_none_readable), long = true)
        }
    }

    BackHandler(enabled = !isCreating && cropRequest == null, onBack = onDismiss)

    Dialog(
        onDismissRequest = { if (!isCreating && cropRequest == null) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        OverlaySystemBars()
        Surface(modifier = modifier.fillMaxSize(), color = scheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !isCreating) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_close),
                            tint = scheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.imgpdf_review_title),
                            style = typography.titleLarge,
                            color = scheme.onSurface
                        )
                        Text(
                            text = pluralStringResource(R.plurals.imgpdf_page_count, pages.size, pages.size),
                            style = typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                when {
                    isImporting -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = scheme.primary)
                        }
                    }

                    pages.isEmpty() -> {
                        EmptyEditorState(
                            onPick = { addImagesLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }

                    else -> {
                        val pagerState = rememberPagerState(pageCount = { pages.size })
                        LaunchedEffect(pages.size) {
                            if (pagerState.currentPage > pages.lastIndex) {
                                pagerState.scrollToPage(pages.lastIndex)
                            }
                        }
                        val currentIndex = pagerState.currentPage.coerceIn(0, pages.lastIndex)
                        val currentPage = pages[currentIndex]

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            pageSpacing = 12.dp
                        ) { index ->
                            pages.getOrNull(index)?.let { page ->
                                PagePreview(
                                    page = page,
                                    pageNumber = index + 1,
                                    total = pages.size,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(pages, key = { _, page -> page.file.absolutePath }) { index, page ->
                                PageThumbnail(
                                    page = page,
                                    index = index,
                                    selected = index == currentIndex,
                                    onClick = { scope.launch { pagerState.scrollToPage(index) } }
                                )
                            }
                            item(key = "add_images") {
                                AddImagesTile(onClick = { addImagesLauncher.launch(arrayOf("image/*")) })
                            }
                        }

                        PageActionsRow(
                            canMoveEarlier = currentIndex > 0,
                            canMoveLater = currentIndex < pages.lastIndex,
                            canRemove = pages.size > 1,
                            onRotate = { delta ->
                                pages[currentIndex] = currentPage.copy(
                                    rotationDegrees = (currentPage.rotationDegrees + delta + 360) % 360
                                )
                            },
                            onCrop = {
                                scope.launch {
                                    val bitmap = withContext(Dispatchers.Default) {
                                        renderUpright(currentPage.file, currentPage.rotationDegrees, CROP_MAX_DIM)
                                    }
                                    if (bitmap != null) {
                                        cropRequest = CropRequest(currentIndex, bitmap)
                                    } else {
                                        context.showMessage(resources.getString(R.string.imgpdf_none_readable), long = true)
                                    }
                                }
                            },
                            onMove = { delta ->
                                val to = (currentIndex + delta).coerceIn(0, pages.lastIndex)
                                if (to != currentIndex) {
                                    pages.add(to, pages.removeAt(currentIndex))
                                    scope.launch { pagerState.scrollToPage(to) }
                                }
                            },
                            onRemove = {
                                pages.removeAt(currentIndex)
                                currentPage.file.delete()
                            }
                        )

                        EditorOptions(
                            page = currentPage,
                            options = options,
                            onPageFilterChange = { filter ->
                                pages[currentIndex] = currentPage.copy(filter = filter)
                            },
                            onOptionsChange = onOptionsChange
                        )

                        Button(
                            onClick = {
                                if (isCreating || pages.isEmpty()) return@Button
                                isCreating = true
                                // Snapshot on the main thread so background baking never reads the
                                // SnapshotStateList while a swipe or edit mutates it.
                                val snapshot = pages.toList()
                                scope.launch {
                                    val baked = withContext(Dispatchers.Default) {
                                        snapshot.map { page ->
                                            if (!page.needsBake) {
                                                page.file
                                            } else {
                                                val bitmap = renderFinal(page) ?: return@map null
                                                val target = File(sessionDir, "baked_${System.nanoTime()}.jpg")
                                                val written = writeJpeg(bitmap, target)
                                                bitmap.recycle()
                                                if (written) target else null
                                            }
                                        }
                                    }
                                    val readable = baked.filterNotNull()
                                    isCreating = false
                                    if (readable.isEmpty()) {
                                        context.showMessage(resources.getString(R.string.imgpdf_none_readable), long = true)
                                        return@launch
                                    }
                                    if (readable.size < snapshot.size) {
                                        context.showMessage(
                                            resources.getString(R.string.imgpdf_some_unreadable, snapshot.size - readable.size),
                                            long = true
                                        )
                                    }
                                    val uris = readable.mapNotNull { file ->
                                        runCatching {
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                        }.getOrNull()
                                    }
                                    if (uris.isEmpty()) {
                                        context.showMessage(resources.getString(R.string.imgpdf_none_readable), long = true)
                                        return@launch
                                    }
                                    onCreate(uris)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .height(52.dp),
                            shape = AppRadius.cardShape
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = scheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(R.string.imgpdf_creating), style = typography.titleSmall)
                            } else {
                                Text(stringResource(R.string.imgpdf_create_pdf), style = typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }

        val request = cropRequest
        if (request != null) {
            FreeCornersCropperDialog(
                bitmap = request.bitmap,
                onDismiss = {
                    if (!request.bitmap.isRecycled) request.bitmap.recycle()
                    cropRequest = null
                },
                onCropped = { cropped ->
                    cropRequest = null
                    if (!request.bitmap.isRecycled) request.bitmap.recycle()
                    scope.launch {
                        val page = pages.getOrNull(request.index)
                        val target = File(sessionDir, "crop_${System.nanoTime()}.jpg")
                        val written = withContext(Dispatchers.IO) { writeJpeg(cropped, target) }
                        cropped.recycle()
                        if (page == null || !written) {
                            context.showMessage(resources.getString(R.string.imgpdf_crop_failed), long = true)
                            return@launch
                        }
                        // The crop was taken from the rotated render, so it is upright already;
                        // the filter stays selected and is applied when the PDF is created.
                        pages[request.index] = page.copy(file = target, rotationDegrees = 0)
                        if (page.file != target) page.file.delete()
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyEditorState(onPick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.imgpdf_empty_title),
            style = typography.titleMedium,
            color = scheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.imgpdf_empty_body),
            style = typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onPick, shape = AppRadius.cardShape) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.imgpdf_pick_images), style = typography.titleSmall)
        }
    }
}

@Composable
private fun PagePreview(
    page: EditorPage,
    pageNumber: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val bitmap by produceState<Bitmap?>(initialValue = null, page.file, page.rotationDegrees) {
        value = withContext(Dispatchers.Default) {
            renderUpright(page.file, page.rotationDegrees, PREVIEW_MAX_DIM)
        }
    }
    Box(
        modifier = modifier
            .clip(AppRadius.cardShape)
            .background(scheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center
    ) {
        val frame = bitmap
        if (frame != null && !frame.isRecycled) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentScale = ContentScale.Fit,
                // The live preview approximates the scan filter; the bake uses the engine.
                colorFilter = ScanFilterEngine.getComposeColorFilter(page.filter)
            )
        } else {
            CircularProgressIndicator(color = scheme.primary, modifier = Modifier.size(28.dp))
        }
        Surface(
            shape = AppRadius.pillShape,
            color = Color.Black.copy(alpha = 0.55f),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.imgpdf_page_of, pageNumber, total),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PageThumbnail(
    page: EditorPage,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bitmap by produceState<Bitmap?>(initialValue = null, page.file, page.rotationDegrees) {
        value = withContext(Dispatchers.Default) {
            renderUpright(page.file, page.rotationDegrees, THUMB_MAX_DIM)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(AppRadius.insetShape)
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 68.dp)
                .clip(AppRadius.insetShape)
                .background(scheme.surfaceContainerHigh)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) scheme.primary else scheme.outlineVariant,
                    shape = AppRadius.insetShape
                )
        ) {
            val frame = bitmap
            if (frame != null && !frame.isRecycled) {
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ScanFilterEngine.getComposeColorFilter(page.filter)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddImagesTile(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 68.dp)
            .clip(AppRadius.insetShape)
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, scheme.outlineVariant, AppRadius.insetShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = stringResource(R.string.imgpdf_add_images),
            tint = scheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun PageActionsRow(
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    canRemove: Boolean,
    onRotate: (Int) -> Unit,
    onCrop: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageAction(Icons.AutoMirrored.Outlined.RotateLeft, R.string.imgpdf_rotate_left) { onRotate(-90) }
        PageAction(Icons.AutoMirrored.Outlined.RotateRight, R.string.imgpdf_rotate_right) { onRotate(90) }
        PageAction(Icons.Outlined.Crop, R.string.imgpdf_crop, onClick = onCrop)
        PageAction(
            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            R.string.imgpdf_move_left,
            enabled = canMoveEarlier
        ) { onMove(-1) }
        PageAction(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            R.string.imgpdf_move_right,
            enabled = canMoveLater
        ) { onMove(1) }
        PageAction(
            Icons.Outlined.DeleteOutline,
            R.string.imgpdf_remove_page,
            enabled = canRemove,
            tint = MaterialTheme.colorScheme.error
        ) { onRemove() }
    }
}

@Composable
private fun PageAction(
    icon: ImageVector,
    labelRes: Int,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    val description = stringResource(labelRes)
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun EditorOptions(
    page: EditorPage,
    options: ToolOptions,
    onPageFilterChange: (ScanFilter) -> Unit,
    onOptionsChange: ((ToolOptions) -> ToolOptions) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        EditorSectionLabel(stringResource(R.string.imgpdf_section_filters))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScrollWithFade(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
        ) {
            ScanFilter.entries.forEach { filter ->
                AppFilterChip(
                    label = filter.label,
                    isSelected = page.filter == filter,
                    onClick = { onPageFilterChange(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        EditorSectionLabel(stringResource(R.string.imgpdf_section_layout))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScrollWithFade(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
        ) {
            PageSizeFormat.entries.forEach { format ->
                AppFilterChip(
                    label = format.label,
                    isSelected = options.pageSizeFormat == format,
                    onClick = { onOptionsChange { it.copy(pageSizeFormat = format) } }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScrollWithFade(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
        ) {
            PageOrientation.entries.forEach { orientation ->
                AppFilterChip(
                    label = orientation.label,
                    isSelected = options.pageOrientation == orientation,
                    onClick = { onOptionsChange { it.copy(pageOrientation = orientation) } }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScrollWithFade(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
        ) {
            PageMargin.entries.forEach { margin ->
                AppFilterChip(
                    label = margin.label,
                    isSelected = options.pageMargin == margin,
                    onClick = { onOptionsChange { it.copy(pageMargin = margin) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        EditorSectionLabel(stringResource(R.string.imgpdf_section_output))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScrollWithFade(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
        ) {
            ImagePdfQuality.entries.forEach { quality ->
                AppFilterChip(
                    label = quality.label,
                    isSelected = options.imagePdfQuality == quality,
                    onClick = { onOptionsChange { it.copy(imagePdfQuality = quality) } }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.opt_compress_images),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
            Switch(
                checked = options.imagePdfCompress,
                onCheckedChange = { compress -> onOptionsChange { it.copy(imagePdfCompress = compress) } }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun EditorSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun copyToSession(context: Context, uri: Uri, dir: File): File? = runCatching {
    dir.mkdirs()
    val rawName = queryDisplayName(context, uri)?.substringAfterLast('/') ?: "image"
    val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(64).ifBlank { "image" }
    val target = File(dir, "src_${System.nanoTime()}_$safeName")
    val opened = context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
        true
    } ?: false
    if (!opened || target.length() == 0L) {
        target.delete()
        null
    } else {
        target
    }
}.getOrNull()

private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) cursor.getString(index) else null
                } else {
                    null
                }
            }
    } else {
        uri.lastPathSegment
    }
}.getOrNull()

/** Sampled decode with EXIF orientation applied, then an editor rotation on top. */
private fun renderUpright(file: File, rotationDegrees: Int, maxDim: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample.toLong() * 2) >= maxDim &&
        sample <= MAX_SAMPLE_SIZE
    ) {
        sample *= 2
    }

    var bitmap = BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return null

    val exif = runCatching { ExifInterface(file) }.getOrNull()
    val exifRotation = exif?.rotationDegrees ?: 0
    val flipped = exif?.isFlipped ?: false
    val totalRotation = (exifRotation + rotationDegrees) % 360
    if (totalRotation != 0 || flipped) {
        val matrix = Matrix().apply {
            if (flipped) setScale(-1f, 1f)
            postRotate(totalRotation.toFloat())
        }
        val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (transformed !== bitmap) {
            bitmap.recycle()
            bitmap = transformed
        }
    }
    return bitmap
}

private suspend fun renderFinal(page: EditorPage): Bitmap? {
    var bitmap = renderUpright(page.file, page.rotationDegrees, BAKE_MAX_DIM) ?: return null
    if (page.filter != ScanFilter.ORIGINAL) {
        val filtered = ScanFilterEngine.applyFilter(bitmap, page.filter)
        if (filtered !== bitmap) bitmap.recycle()
        bitmap = filtered
    }
    return bitmap
}

private fun writeJpeg(bitmap: Bitmap, target: File): Boolean = runCatching {
    FileOutputStream(target).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
}.getOrDefault(false)
