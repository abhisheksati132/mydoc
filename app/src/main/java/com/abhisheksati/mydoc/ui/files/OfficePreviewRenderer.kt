package com.abhisheksati.mydoc.ui.files

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import com.abhisheksati.mydoc.rendering.engine.word.Word97Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Renders a first-page thumbnail from the document's own content when the optional Office engine
 * is not installed, so Word and PowerPoint files no longer fall back to a type plate. The layout
 * is an honest approximation - typography and reading order, not pagination.
 */
internal object OfficePreviewRenderer {

    suspend fun render(extension: String, cacheDir: File?, open: () -> InputStream?): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                when (extension.lowercase()) {
                    "docx", "dotx", "odt", "ott", "fodt", "rtf" -> renderWord(open)
                    "doc", "dot" -> renderLegacyWord(open)
                    "pptx", "pptm", "ppsx", "potx" -> renderSlides(cacheDir, open)
                    else -> null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }

    private class PreviewLine(val text: String, val kind: LineKind, val marker: String? = null)

    private enum class LineKind { TITLE, HEADING, BODY }

    private suspend fun renderWord(open: () -> InputStream?): Bitmap? {
        val stream = open() ?: return null
        val document = stream.use { DocxParser().parse(it) }
        val lines = mutableListOf<PreviewLine>()
        for (block in document.blocks) {
            when (block) {
                is DocxBlock.Heading -> block.text.trim().takeIf { it.isNotEmpty() }?.let {
                    lines += PreviewLine(it, if (block.level <= 1) LineKind.TITLE else LineKind.HEADING)
                }
                is DocxBlock.Paragraph -> block.fullText.trim().takeIf { it.isNotEmpty() }?.let {
                    lines += PreviewLine(it, LineKind.BODY)
                }
                is DocxBlock.ListItem -> block.fullText.trim().takeIf { it.isNotEmpty() }?.let {
                    lines += PreviewLine(it, LineKind.BODY, block.marker.take(3))
                }
                is DocxBlock.Table -> block.rows.take(3).forEach { row ->
                    val text = row.joinToString("  |  ") { it.text }.trim()
                    if (text.isNotEmpty()) lines += PreviewLine(text, LineKind.BODY)
                }
                is DocxBlock.Image, DocxBlock.PageBreak -> Unit
            }
            if (lines.size >= MAX_LINES) break
        }
        if (lines.isEmpty()) return null
        return drawWordPage(lines)
    }

    private fun renderLegacyWord(open: () -> InputStream?): Bitmap? {
        // The cap has to bound the allocation itself. Reading the whole stream first and
        // checking its length afterwards let a large .doc throw OutOfMemoryError, which the
        // renderer's catch(Exception) does not handle.
        val bytes = open()?.use { readBounded(it, MAX_LEGACY_WORD_BYTES) } ?: return null
        if (bytes.isEmpty()) return null
        val text = Word97Text.read(bytes) ?: return null
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_LINES)
            .mapIndexed { index, line -> PreviewLine(line, if (index == 0) LineKind.TITLE else LineKind.BODY) }
            .toList()
        if (lines.isEmpty()) return null
        return drawWordPage(lines)
    }

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16_384)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (out.size() + read > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private suspend fun renderSlides(cacheDir: File?, open: () -> InputStream?): Bitmap? {
        val stream = open() ?: return null
        val presentation = stream.use { PptxParser().parse(it, cacheDir) }
        val slide = presentation.slides.firstOrNull() ?: return null
        val title = slide.title.trim()
        val bullets = slide.content.map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_BULLETS)
        if (title.isEmpty() && bullets.isEmpty()) return null
        return drawSlide(title, bullets, slide.aspectRatio, slide.backgroundColor?.toArgb())
    }

    private fun drawWordPage(lines: List<PreviewLine>): Bitmap {
        val width = 620
        val height = 877
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val margin = 54f
        val textWidth = width - (margin * 2).toInt()
        var y = 60f
        for (line in lines) {
            if (y > height - margin) break
            val paint = wordPaint(line.kind)
            val body = if (line.marker != null) "${line.marker}  ${line.text}" else line.text
            val layout = layoutOf(body, paint, textWidth)
            if (y + layout.height > height - margin) break
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + when (line.kind) {
                LineKind.TITLE -> 14f
                LineKind.HEADING -> 8f
                LineKind.BODY -> 6f
            }
        }
        return bitmap
    }

    private fun wordPaint(kind: LineKind): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(26, 23, 18)
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        when (kind) {
            LineKind.TITLE -> {
                textSize = 34f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            LineKind.HEADING -> {
                textSize = 24f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            LineKind.BODY -> textSize = 18f
        }
    }

    private fun drawSlide(title: String, bullets: List<String>, aspectRatio: Float, background: Int?): Bitmap {
        val width = 800
        val ratio = aspectRatio.takeIf { it in 0.5f..2.5f } ?: (16f / 9f)
        val height = (width / ratio).roundToInt().coerceIn(400, 720)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background?.takeIf { Color.alpha(it) == 255 } ?: Color.WHITE)
        val margin = 64f
        var y = 72f
        if (title.isNotEmpty()) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(26, 23, 18)
                textSize = 44f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val layout = layoutOf(title, paint, width - (margin * 2).toInt())
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 28f
        }
        if (bullets.isNotEmpty()) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(64, 60, 52)
                textSize = 26f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            for (bullet in bullets) {
                if (y > height - margin) break
                val layout = layoutOf("\u2022  $bullet", paint, width - (margin * 2).toInt())
                if (y + layout.height > height - margin) break
                canvas.save()
                canvas.translate(margin, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + 14f
            }
        }
        return bitmap
    }

    private fun layoutOf(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1f)
            .setIncludePad(false)
            .build()

    private const val MAX_LINES = 34
    private const val MAX_BULLETS = 6
    private const val MAX_LEGACY_WORD_BYTES = 50 * 1024 * 1024
}
