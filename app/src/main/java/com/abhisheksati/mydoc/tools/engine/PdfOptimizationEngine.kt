package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import com.abhisheksati.mydoc.tools.model.CompressionLevel
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFDocument
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Specialized engine handling document optimization, multi-strategy compression,
 * stream compaction, and damaged xref table reconstruction / repairs.
 */
class PdfOptimizationEngine(private val context: Context) {

    private fun createTempOutputFile(prefix: String, ext: String): File =
        PdfToolUtils.createTempOutputFile(context.cacheDir, prefix, ext)

    suspend fun compressPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val originalSize = file.length()
        onProgress(ToolProgress(0, 100, 10, "Analyzing document streams and images...", false))

        // Strategy A: Recompress embedded image streams with PDFBox
        var totalImagesRecompressed = 0
        val boxOutFile = createTempOutputFile("compressed_box", "pdf")
        var pdfBoxSucceeded = false

        try {
            PdfBoxHelper.ensureInitialized(context)
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    val totalPages = doc.numberOfPages
                    val (maxDim, qualityFloat) = when (options.compressionLevel) {
                        CompressionLevel.LIGHT -> Pair(1600, 0.75f)
                        CompressionLevel.BALANCED -> Pair(1200, 0.60f)
                        CompressionLevel.STRONG -> Pair(800, 0.45f)
                    }

                    for (p in 0 until totalPages) {
                        currentCoroutineContext().ensureActive()
                        val page = doc.getPage(p)
                        val resources = page.resources ?: continue
                        for (name in resources.xObjectNames) {
                            if (resources.isImageXObject(name)) {
                                val xobj = resources.getXObject(name) as? PDImageXObject ?: continue
                                val origBmp = runCatching { xobj.image }.getOrNull() ?: continue
                                val w = origBmp.width
                                val h = origBmp.height

                                val scale = minOf(1.0f, maxDim.toFloat() / maxOf(w, h).toFloat())
                                val targetW = (w * scale).toInt().coerceAtLeast(1)
                                val targetH = (h * scale).toInt().coerceAtLeast(1)

                                val scaledBmp = if (targetW != w || targetH != h) {
                                    Bitmap.createScaledBitmap(origBmp, targetW, targetH, true)
                                } else {
                                    origBmp
                                }

                                val newImg = JPEGFactory.createFromImage(doc, scaledBmp, qualityFloat)
                                resources.put(name, newImg)
                                totalImagesRecompressed++

                                if (scaledBmp !== origBmp) {
                                    scaledBmp.recycle()
                                }
                            }
                        }
                        val pct = 10 + ((p.toFloat() / totalPages) * 35).toInt()
                        onProgress(ToolProgress(p + 1, totalPages, pct, "Optimizing image streams...", false))
                    }

                    if (totalImagesRecompressed > 0) {
                        FileOutputStream(boxOutFile).use { fos -> doc.save(fos) }
                        pdfBoxSucceeded = true
                    }
                }
            }
        } catch (_: Exception) {
            pdfBoxSucceeded = false
        }

        // Strategy B: Scanned / photo-heavy page compression via MuPDF & AndroidPdfDocument
        val rasterOutFile = createTempOutputFile("compressed_raster", "pdf")
        var rasterSucceeded = false

        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
        if (srcDoc != null) {
            try {
                val totalPages = srcDoc.countPages()
                val (renderScale, jpegQuality) = when (options.compressionLevel) {
                    CompressionLevel.LIGHT -> Pair(1.5f, 75)
                    CompressionLevel.BALANCED -> Pair(1.15f, 60)
                    CompressionLevel.STRONG -> Pair(0.85f, 45)
                }

                val outDoc = AndroidPdfDocument()
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    val page = srcDoc.loadPage(p)
                    val b = page.bounds
                    val w = (b.x1 - b.x0).toInt().coerceAtLeast(1)
                    val h = (b.y1 - b.y0).toInt().coerceAtLeast(1)

                    val pixmap = page.toPixmap(Matrix(renderScale, renderScale), ColorSpace.DeviceRGB, false)
                    val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    pixmap.destroy()
                    page.destroy()

                    val baos = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
                    bmp.recycle()

                    val pageInfo = AndroidPdfDocument.PageInfo.Builder(w, h, p + 1).create()
                    val aPage = outDoc.startPage(pageInfo)
                    val compressedBmp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size())
                    if (compressedBmp != null) {
                        aPage.canvas.drawBitmap(compressedBmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                        compressedBmp.recycle()
                    }
                    outDoc.finishPage(aPage)

                    val pct = 45 + ((p.toFloat() / totalPages) * 45).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Compressing page ${p + 1} of $totalPages...", false))
                }

                FileOutputStream(rasterOutFile).use { outDoc.writeTo(it) }
                outDoc.close()
                rasterSucceeded = true
            } catch (_: Exception) {
                rasterSucceeded = false
            }

            // Strategy C: Pure object and xref compaction via MuPDF
            val cleanOutFile = createTempOutputFile("compressed_clean", "pdf")
            val cleanParams = when (options.compressionLevel) {
                CompressionLevel.LIGHT -> "compress,clean,garbage=2"
                CompressionLevel.BALANCED -> "compress,deflate,clean,garbage=3"
                CompressionLevel.STRONG -> "compress,deflate,clean,garbage=4"
            }
            try {
                srcDoc.save(cleanOutFile.absolutePath, cleanParams)
            } catch (_: Exception) {}
            srcDoc.destroy()

            onProgress(ToolProgress(100, 100, 95, "Selecting most efficient compression...", false))

            // Choose the smallest resulting file that is smaller than original!
            val candidates = mutableListOf<File>()
            if (pdfBoxSucceeded && boxOutFile.exists() && boxOutFile.length() > 0 && boxOutFile.length() < originalSize) {
                candidates.add(boxOutFile)
            }
            if (rasterSucceeded && rasterOutFile.exists() && rasterOutFile.length() > 0 && rasterOutFile.length() < originalSize * 0.90f) {
                candidates.add(rasterOutFile)
            }
            if (cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() < originalSize) {
                candidates.add(cleanOutFile)
            }

            val bestFile = candidates.minByOrNull { it.length() }
            if (bestFile != null) {
                if (bestFile != boxOutFile && boxOutFile.exists()) boxOutFile.delete()
                if (bestFile != rasterOutFile && rasterOutFile.exists()) rasterOutFile.delete()
                if (bestFile != cleanOutFile && cleanOutFile.exists()) cleanOutFile.delete()
                return bestFile
            }

            if (cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() <= originalSize) {
                if (boxOutFile.exists()) boxOutFile.delete()
                if (rasterOutFile.exists()) rasterOutFile.delete()
                return cleanOutFile
            }
        }

        if (boxOutFile.exists() && boxOutFile.length() > 0) return boxOutFile
        return file
    }

    suspend fun repairPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        onProgress(ToolProgress(1, 100, 20, "Rebuilding damaged xref tables...", false))
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
        if (srcDoc != null) {
            try {
                val totalPages = srcDoc.countPages()
                val destDoc = PDFDocument()

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    try {
                        destDoc.graftPage(destDoc.countPages(), srcDoc, p)
                    } catch (_: Exception) {
                        // Skip corrupt page gracefully
                    }
                    val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Recovering page ${p + 1}...", false))
                }

                val outFile = createTempOutputFile("repaired", "pdf")
                destDoc.save(outFile.absolutePath, "compress,clean,garbage=4")
                destDoc.destroy()
                return outFile
            } finally {
                srcDoc.destroy()
            }
        } else {
            // Secondary PDFBox recovery fallback
            PdfBoxHelper.ensureInitialized(context)
            val outFile = createTempOutputFile("repaired_box", "pdf")
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    FileOutputStream(outFile).use { fos -> doc.save(fos) }
                }
            }
            return outFile
        }
    }

}
