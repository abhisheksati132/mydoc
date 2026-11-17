package com.abhisheksati.mydoc.tools.notes

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.abhisheksati.mydoc.data.local.entity.HighlightEntity
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Turns saved highlights into a shareable Markdown or clean PDF study sheet. */
object NotesExporter {

    fun writeMarkdown(context: Context, documentName: String, highlights: List<HighlightEntity>): File {
        val file = outputFile(context, documentName, "md")
        file.writeText(buildMarkdown(documentName, highlights))
        return file
    }

    fun buildMarkdown(documentName: String, highlights: List<HighlightEntity>): String = buildString {
        appendLine("# Notes — $documentName")
        appendLine()
        appendLine("_Exported ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date())}_")
        appendLine()
        if (highlights.isEmpty()) {
            appendLine("No highlights saved yet.")
            return@buildString
        }
        var lastPage = -1
        for (highlight in highlights) {
            if (highlight.pageIndex != lastPage) {
                lastPage = highlight.pageIndex
                appendLine("## Page ${highlight.pageIndex + 1}")
                appendLine()
            }
            highlight.text.lines().forEach { line -> appendLine("> $line") }
            highlight.note?.takeIf { it.isNotBlank() }?.let { note ->
                appendLine()
                appendLine("**Note:** $note")
            }
            appendLine()
        }
    }

    fun writePdf(context: Context, documentName: String, highlights: List<HighlightEntity>): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val writer = PageWriter(document, paint)
        writer.heading("Notes — $documentName", 20f)
        writer.body(
            "Exported ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date())} · ${highlights.size} highlight(s)",
            10f,
            Color.GRAY
        )
        if (highlights.isEmpty()) {
            writer.body("No highlights saved yet.", 12f, Color.DKGRAY)
        } else {
            var lastPage = -1
            for (highlight in highlights) {
                if (highlight.pageIndex != lastPage) {
                    lastPage = highlight.pageIndex
                    writer.heading("Page ${highlight.pageIndex + 1}", 14f)
                }
                highlight.text.lines().forEach { line -> writer.body("  $line", 11f, Color.BLACK) }
                highlight.note?.takeIf { it.isNotBlank() }?.let { note ->
                    writer.body("Note: $note", 10f, Color.DKGRAY)
                }
                writer.spacer(8f)
            }
        }
        val file = outputFile(context, documentName, "pdf")
        // PdfDocument holds native page buffers; an I/O failure (full disk, revoked path)
        // must not skip their release.
        try {
            writer.finish()
            file.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return file
    }

    private fun outputFile(context: Context, documentName: String, extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val base = documentName.substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim()
            .take(80)
            .ifBlank { "document" }
        val file = File(dir, "${base}_notes.$extension")
        runCatching { file.delete() }
        return file
    }

    private class PageWriter(private val document: PdfDocument, private val paint: Paint) {
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = MARGIN

        fun heading(text: String, size: Float) {
            y += size * 0.5f
            drawWrapped(text, size, Color.BLACK, bold = true)
            spacer(4f)
        }

        fun body(text: String, size: Float, color: Int) {
            drawWrapped(text, size, color, bold = false)
        }

        fun spacer(height: Float) {
            y += height
        }

        fun finish() {
            page?.let { document.finishPage(it) }
            page = null
        }

        private fun drawWrapped(text: String, size: Float, color: Int, bold: Boolean) {
            paint.textSize = size
            paint.color = color
            paint.isFakeBoldText = bold
            val lineHeight = size * 1.35f
            for (paragraph in text.split('\n')) {
                var remaining = paragraph
                if (remaining.isEmpty()) {
                    y += lineHeight * 0.5f
                    continue
                }
                while (remaining.isNotEmpty()) {
                    if (page == null || y + lineHeight > PAGE_HEIGHT - MARGIN) newPage()
                    val count = paint.breakText(remaining, true, CONTENT_WIDTH, null).coerceAtLeast(1)
                    val line = remaining.take(count)
                    page!!.canvas.drawText(line, MARGIN, y, paint)
                    y += lineHeight
                    remaining = remaining.substring(count)
                }
            }
        }

        private fun newPage() {
            finish()
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
            )
            page!!.canvas.drawColor(Color.WHITE)
            y = MARGIN
        }
    }

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
}
