package com.abhisheksati.mydoc.designsystem.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.abhisheksati.mydoc.data.local.DocumentStreamResolver
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, offline memory + disk cache for first-page document cover thumbnails.
 */
object DocumentCoverCache {
    private val memoryCache = LruCache<String, Bitmap>(60)

    fun getFromMemory(key: String): Bitmap? = memoryCache.get(key)

    fun putInMemory(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
    }

    suspend fun loadCover(
        context: Context,
        document: DocumentFile,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${document.uri.hashCode()}_${targetWidthPx}x${targetHeightPx}"
        getFromMemory(cacheKey)?.let { return@withContext it }

        val cacheDir = File(context.cacheDir, "doc_covers").apply { mkdirs() }
        val diskFile = File(cacheDir, "$cacheKey.webp")

        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bmp != null) {
                    putInMemory(cacheKey, bmp)
                    return@withContext bmp
                }
            } catch (_: Exception) {}
        }

        // Render first page if document is PDF
        val ext = document.extension.lowercase()
        if (ext == "pdf") {
            try {
                val uri = Uri.parse(document.uri)
                val tempFile = File(cacheDir, "temp_${document.uri.hashCode()}.pdf")
                val resolved = DocumentStreamResolver.resolveToCacheFile(
                    context = context,
                    uri = uri,
                    displayName = document.displayName,
                    targetCacheFile = tempFile
                )

                if (resolved.isSuccess) {
                    val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val pdfRenderer = PdfRenderer(pfd)
                    if (pdfRenderer.pageCount > 0) {
                        val page = pdfRenderer.openPage(0)
                        val w = if (targetWidthPx > 0) targetWidthPx else 240
                        val h = if (targetHeightPx > 0) targetHeightPx else (w * page.height / page.width).coerceAtLeast(100)

                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pdfRenderer.close()
                        pfd.close()

                        // Save to disk cache
                        try {
                            FileOutputStream(diskFile).use { out ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                        } catch (_: Exception) {}

                        tempFile.delete()
                        putInMemory(cacheKey, bmp)
                        return@withContext bmp
                    }
                    pdfRenderer.close()
                    pfd.close()
                }
            } catch (_: Exception) {}
        }

        null
    }
}

/**
 * A beautiful, book-proportioned Cover Image component for documents.
 * Renders the real first-page cover with depth and spine shadow when available,
 * or an elegant typographic document jacket when loading or unsupported.
 */
@Composable
fun DocumentCoverImage(
    document: DocumentFile,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 0.75f, // 3:4 book aspect ratio
    cornerRadius: Dp = 8.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val targetWidthPx = with(density) { 120.dp.roundToPx() }
    val targetHeightPx = with(density) { (120.dp / aspectRatio).roundToPx() }

    val coverBitmap by produceState<Bitmap?>(initialValue = null, key1 = document.uri) {
        val cached = DocumentCoverCache.getFromMemory("${document.uri.hashCode()}_${targetWidthPx}x${targetHeightPx}")
        if (cached != null) {
            value = cached
        } else {
            value = DocumentCoverCache.loadCover(context, document, targetWidthPx, targetHeightPx)
        }
    }

    val isDark = colors.background == DarkAppColors.background
    val badgeColor = DocumentFormatColors.getTint(document.typeBadge)
    val badgeBg = DocumentFormatColors.getContainer(document.typeBadge, isDark)

    Surface(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = colors.surfaceContainer,
        border = BorderStroke(0.7.dp, colors.cardBorder)
    ) {
        Crossfade(targetState = coverBitmap, label = "coverCrossfade") { bmp ->
            if (bmp != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = document.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle book spine shadow on the left edge for tangible physical feel
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Tiny format tag in bottom-right corner
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = document.typeBadge.uppercase(),
                            style = typography.caption.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }
            } else {
                // Typographic Book Jacket placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    badgeBg,
                                    colors.surfaceContainer
                                )
                            )
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Spine highlight line
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(badgeColor.copy(alpha = 0.4f))
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getFormatIcon(document.typeBadge),
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = document.typeBadge.uppercase(),
                            style = typography.caption.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getFormatIcon(extension: String): ImageVector {
    return when (extension.lowercase()) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "docx", "doc" -> Icons.Outlined.Description
        "xlsx", "xls", "csv" -> Icons.Outlined.TableChart
        "pptx", "ppt" -> Icons.Outlined.Slideshow
        else -> Icons.AutoMirrored.Outlined.Article
    }
}
