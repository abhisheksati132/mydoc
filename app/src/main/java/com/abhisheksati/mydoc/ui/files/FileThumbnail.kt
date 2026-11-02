package com.abhisheksati.mydoc.ui.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.ui.filemanager.getFileTypeIcon
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val pdfThumbnailCache = object : LruCache<String, ImageBitmap>(64) {}
private val textPreviewExtensions = setOf("txt", "md", "csv", "log", "json")

private object TypeTint {
    val pdf = Color(0xFFE5484D)
    val word = Color(0xFF3B6FF5)
    val excel = Color(0xFF2E9E6B)
    val slides = Color(0xFFE07A2F)
    val image = Color(0xFFB45BCB)
    val video = Color(0xFFE2618A)
    val audio = Color(0xFF2AA198)
    val archive = Color(0xFF8A6B2F)
    val apk = Color(0xFF34A853)
    val code = Color(0xFF6B7280)
    val generic = Color(0xFF7A8CA3)
}

private fun tintFor(type: StorageFileType): Color = when (type) {
    StorageFileType.PDF -> TypeTint.pdf
    StorageFileType.WORD -> TypeTint.word
    StorageFileType.EXCEL -> TypeTint.excel
    StorageFileType.PPT -> TypeTint.slides
    StorageFileType.IMAGE -> TypeTint.image
    StorageFileType.VIDEO -> TypeTint.video
    StorageFileType.AUDIO -> TypeTint.audio
    StorageFileType.ARCHIVE -> TypeTint.archive
    StorageFileType.APK -> TypeTint.apk
    StorageFileType.CODE_TEXT -> TypeTint.code
    StorageFileType.GENERIC, StorageFileType.DIRECTORY -> TypeTint.generic
}

/**
 * A real preview where one exists: image content, a rendered PDF first page,
 * or the opening lines of a text file. Everything else is a quiet format plate
 * so the list never invents a page.
 */
@Composable
fun StorageThumbnail(
    item: StorageItem,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    when {
        item.fileType == StorageFileType.IMAGE -> Box(
            modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp))
        ) {
            AsyncImage(
                model = item.file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        item.fileType == StorageFileType.PDF -> PdfThumbnail(
            key = item.path,
            modifier = modifier,
            cornerRadius = cornerRadius,
            load = { item.file }
        )
        item.fileType == StorageFileType.CODE_TEXT -> TextThumbnail(
            key = item.path,
            modifier = modifier,
            cornerRadius = cornerRadius,
            load = { readTextFile(item.file) }
        )
        else -> TypePlate(
            type = item.fileType,
            modifier = modifier,
            cornerRadius = cornerRadius
        )
    }
}

@Composable
fun LibraryThumbnail(
    item: LibraryItem,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    val context = LocalContext.current
    when {
        item.kind == MediaKind.PHOTO -> Box(
            modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp))
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        item.extension == "pdf" -> PdfThumbnail(
            key = item.uri,
            modifier = modifier,
            cornerRadius = cornerRadius,
            load = { findLibraryPdf(context, item) }
        )
        item.extension in textPreviewExtensions -> TextThumbnail(
            key = item.uri,
            modifier = modifier,
            cornerRadius = cornerRadius,
            load = { readLibraryText(context, item) }
        )
        else -> TypePlate(
            type = when (item.kind) {
                MediaKind.PHOTO -> StorageFileType.IMAGE
                MediaKind.VIDEO -> StorageFileType.VIDEO
                MediaKind.AUDIO -> StorageFileType.AUDIO
                MediaKind.DOCUMENT -> genericTypeFor(item.extension)
            },
            badge = item.typeBadge,
            modifier = modifier,
            cornerRadius = cornerRadius
        )
    }
}

private fun genericTypeFor(extension: String): StorageFileType = when (extension) {
    "doc", "docx" -> StorageFileType.WORD
    "xls", "xlsx" -> StorageFileType.EXCEL
    "ppt", "pptx" -> StorageFileType.PPT
    "zip", "rar", "7z" -> StorageFileType.ARCHIVE
    "apk" -> StorageFileType.APK
    "txt", "md", "csv", "log", "json" -> StorageFileType.CODE_TEXT
    else -> StorageFileType.GENERIC
}

@Composable
private fun TypePlate(
    type: StorageFileType,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    badge: String? = null
) {
    val tint = tintFor(type)
    val container = tint.copy(alpha = 0.14f)
    Box(
        modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        if (badge != null) {
            Text(
                text = badge,
                color = tint,
                fontSize = 10.sp,
                style = MaterialTheme.typography.labelSmall
            )
        } else {
            Icon(
                imageVector = getFileTypeIcon(type),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PdfThumbnail(
    key: String,
    modifier: Modifier,
    cornerRadius: Int,
    load: () -> File?
) {
    var bitmap by remember(key) { mutableStateOf(pdfThumbnailCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) bitmap = loadPdfThumbnail(key, load)
    }
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = getFileTypeIcon(StorageFileType.PDF),
                contentDescription = null,
                tint = TypeTint.pdf,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TextThumbnail(
    key: String,
    modifier: Modifier,
    cornerRadius: Int,
    load: () -> String?
) {
    var bitmap by remember(key) { mutableStateOf(pdfThumbnailCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) bitmap = loadTextThumbnail(key, load)
    }
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = getFileTypeIcon(StorageFileType.CODE_TEXT),
                contentDescription = null,
                tint = TypeTint.code,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private suspend fun loadPdfThumbnail(key: String, load: () -> File?): ImageBitmap? =
    withContext(Dispatchers.IO) {
        pdfThumbnailCache.get(key)?.let { return@withContext it }
        val file = runCatching(load).getOrNull() ?: return@withContext null
        val bitmap = renderFirstPage(file) ?: return@withContext null
        bitmap.asImageBitmap().also { pdfThumbnailCache.put(key, it) }
    }

private suspend fun loadTextThumbnail(key: String, load: () -> String?): ImageBitmap? =
    withContext(Dispatchers.IO) {
        pdfThumbnailCache.get(key)?.let { return@withContext it }
        val text = runCatching(load).getOrNull() ?: return@withContext null
        renderTextPage(text).asImageBitmap().also { pdfThumbnailCache.put(key, it) }
    }

private fun renderFirstPage(file: File): Bitmap? {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    try {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
        if (renderer.pageCount == 0) return null
        renderer.openPage(0).use { page ->
            val width = 220
            val height = (page.height.toFloat() / page.width * width).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(AndroidColor.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    } catch (_: Throwable) {
        return null
    } finally {
        renderer?.close()
        pfd?.close()
    }
}

private fun renderTextPage(text: String): Bitmap {
    val width = 220
    val height = 293
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1A1712.toInt()
        textSize = 6.5f
        typeface = Typeface.MONOSPACE
    }
    val left = 16f
    val maxWidth = width - 2 * left
    val lineHeight = paint.textSize * 1.55f
    var y = 22f

    run outer@{
        for (paragraph in text.split('\n')) {
            var line = ""
            for (word in paragraph.split(Regex("\\s+")).filter { it.isNotEmpty() }) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    if (line.isNotEmpty()) {
                        canvas.drawText(line, left, y, paint)
                        y += lineHeight
                    }
                    line = word
                    if (y > height - 12f) return@outer
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, left, y, paint)
                y += lineHeight
            }
            y += lineHeight * 0.6f
            if (y > height - 12f) return@outer
        }
    }
    return bitmap
}

private fun readTextFile(file: File): String? = runCatching {
    if (!file.exists()) return null
    val buffer = CharArray(1200)
    val read = file.reader().use { it.read(buffer) }
    if (read <= 0) null else String(buffer, 0, read).trim()
}.getOrNull()

private fun findLibraryPdf(context: Context, item: LibraryItem): File? {
    val uri = Uri.parse(item.uri)
    if (uri.scheme == "file" || uri.scheme.isNullOrEmpty()) {
        val direct = uri.path?.let(::File)
        if (direct != null && direct.exists() && direct.length() > 0L) return direct
    }
    val key = item.uri.hashCode().toString(16)
    File(context.filesDir, "saved_docs")
        .listFiles()
        ?.firstOrNull { it.name.startsWith(key) && it.length() > 0L }
        ?.let { return it }

    val dir = File(context.cacheDir, "files_thumbs").apply { mkdirs() }
    val target = File(dir, "$key.pdf")
    if (target.exists() && target.length() > 0L) return target
    val stream = DocumentStreamResolver.openStream(context, Uri.parse(item.uri), item.name)
        ?: return null
    stream.use { input -> target.outputStream().use { input.copyTo(it) } }
    return target.takeIf { it.length() > 0L }
}

private fun readLibraryText(context: Context, item: LibraryItem): String? = runCatching {
    DocumentStreamResolver.openStream(context, Uri.parse(item.uri), item.name)?.use { stream ->
        val buffer = CharArray(1200)
        val read = stream.reader().use { reader -> reader.read(buffer) }
        if (read <= 0) null else String(buffer, 0, read).trim()
    }
}.getOrNull()

/** Small fixed format plate used by the section shortcuts. */
val TypeBadgePad = 20.dp
