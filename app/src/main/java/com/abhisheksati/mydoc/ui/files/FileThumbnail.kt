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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.domain.model.LibraryItem
import com.abhisheksati.mydoc.domain.model.MediaKind
import com.abhisheksati.mydoc.domain.model.StorageFileType
import com.abhisheksati.mydoc.domain.model.StorageItem
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getFileTypeIcon(type: StorageFileType): ImageVector = when (type) {
    StorageFileType.DIRECTORY -> Icons.Outlined.Folder
    StorageFileType.PDF -> Icons.Outlined.PictureAsPdf
    StorageFileType.WORD -> Icons.Outlined.Description
    StorageFileType.EXCEL -> Icons.Outlined.TableChart
    StorageFileType.PPT -> Icons.Outlined.Slideshow
    StorageFileType.IMAGE -> Icons.Outlined.Image
    StorageFileType.VIDEO -> Icons.Outlined.Movie
    StorageFileType.AUDIO -> Icons.Outlined.AudioFile
    StorageFileType.ARCHIVE -> Icons.Outlined.FolderZip
    StorageFileType.APK -> Icons.Outlined.Android
    StorageFileType.CODE_TEXT -> Icons.Outlined.Code
    StorageFileType.GENERIC -> Icons.AutoMirrored.Outlined.InsertDriveFile
}

// A tile is ~220x293 ARGB, so a count cap of 64 could pin ~16 MB of bitmaps; the
// bound is in bytes so the grid's footprint does not depend on tile size.
private const val MAX_THUMBNAIL_CACHE_KB = 6 * 1024
private const val MAX_THUMBNAIL_COPY_BYTES = 40L * 1024 * 1024

private val pdfThumbnailCache = object : LruCache<String, ImageBitmap>(MAX_THUMBNAIL_CACHE_KB) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        (value.width * value.height * 4 / 1024).coerceAtLeast(1)
}
private val textPreviewExtensions = setOf("txt", "md", "csv", "log", "json")

private object TypeTint {
    val pdf = Color(0xFFD94324)     // Stamp Vermilion
    val word = Color(0xFF43586B)    // Editorial slate
    val excel = Color(0xFF38634B)   // Sage ink
    val slides = Color(0xFF7D5432)  // Warm bronze
    val image = Color(0xFF5E576B)   // Stone slate
    val video = Color(0xFF6E4D5B)   // Muted plum
    val audio = Color(0xFF3D5E5E)   // Deep spruce
    val archive = Color(0xFF665943) // Kraft parchment
    val apk = Color(0xFF43604C)     // Olive ink
    val code = Color(0xFF4E565F)    // Charcoal
    val generic = Color(0xFF5D646D) // Neutral ink

    // Dark themes lift the same hues to tonal variants so the format plates
    // stay legible against dark containers instead of muddying into them.
    val pdfDark = Color(0xFFE8734F)
    val wordDark = Color(0xFF8FA6BC)
    val excelDark = Color(0xFF7FBF9A)
    val slidesDark = Color(0xFFD69A6C)
    val imageDark = Color(0xFFA99FBE)
    val videoDark = Color(0xFFC98FA6)
    val audioDark = Color(0xFF7FBDBD)
    val archiveDark = Color(0xFFBFAE87)
    val apkDark = Color(0xFF8FBE99)
    val codeDark = Color(0xFF9AA5B1)
    val genericDark = Color(0xFFA7AEB8)
}

private fun tintFor(type: StorageFileType, isDark: Boolean = false): Color = if (isDark) when (type) {
    StorageFileType.PDF -> TypeTint.pdfDark
    StorageFileType.WORD -> TypeTint.wordDark
    StorageFileType.EXCEL -> TypeTint.excelDark
    StorageFileType.PPT -> TypeTint.slidesDark
    StorageFileType.IMAGE -> TypeTint.imageDark
    StorageFileType.VIDEO -> TypeTint.videoDark
    StorageFileType.AUDIO -> TypeTint.audioDark
    StorageFileType.ARCHIVE -> TypeTint.archiveDark
    StorageFileType.APK -> TypeTint.apkDark
    StorageFileType.CODE_TEXT -> TypeTint.codeDark
    StorageFileType.GENERIC, StorageFileType.DIRECTORY -> TypeTint.genericDark
} else when (type) {
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
    modifier: Modifier = Modifier
) {
    when {
        item.fileType == StorageFileType.IMAGE -> Box(
            modifier = modifier.clip(AppRadius.insetShape)
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
            load = { item.file }
        )
        item.fileType == StorageFileType.CODE_TEXT -> TextThumbnail(
            key = item.path,
            modifier = modifier,
            load = { readTextFile(item.file) }
        )
        else -> TypePlate(
            type = item.fileType,
            modifier = modifier
        )
    }
}

@Composable
fun LibraryThumbnail(
    item: LibraryItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    when {
        item.kind == MediaKind.PHOTO -> Box(
            modifier = modifier.clip(AppRadius.insetShape)
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
            load = { findLibraryPdf(context, item) }
        )
        item.extension in textPreviewExtensions -> TextThumbnail(
            key = item.uri,
            modifier = modifier,
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
            modifier = modifier
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
    badge: String? = null
) {
    val colors = com.abhisheksati.mydoc.designsystem.tokens.AppTheme.colors
    val tint = if (type == StorageFileType.PDF) colors.accent else tintFor(type, com.abhisheksati.mydoc.designsystem.tokens.AppTheme.isDark)
    val container = if (type == StorageFileType.PDF) colors.accentContainer else colors.surfaceMuted
    Box(
        modifier = modifier.clip(AppRadius.insetShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        if (badge != null) {
            Text(
                text = badge,
                color = tint,
                style = com.abhisheksati.mydoc.designsystem.tokens.AppTheme.typography.captionStrong
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
    load: () -> File?
) {
    var bitmap by remember(key) { mutableStateOf(pdfThumbnailCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) bitmap = loadPdfThumbnail(key, load)
    }
    Box(
        modifier = modifier
            .clip(AppRadius.insetShape)
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
                tint = tintFor(StorageFileType.PDF, com.abhisheksati.mydoc.designsystem.tokens.AppTheme.isDark),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TextThumbnail(
    key: String,
    modifier: Modifier,
    load: () -> String?
) {
    var bitmap by remember(key) { mutableStateOf(pdfThumbnailCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) bitmap = loadTextThumbnail(key, load)
    }
    Box(
        modifier = modifier
            .clip(AppRadius.insetShape)
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
                tint = tintFor(StorageFileType.CODE_TEXT, com.abhisheksati.mydoc.designsystem.tokens.AppTheme.isDark),
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
    // A 44 dp tile is not worth staging a whole scanned PDF into cache for.
    if (item.sizeBytes > MAX_THUMBNAIL_COPY_BYTES) return null
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
