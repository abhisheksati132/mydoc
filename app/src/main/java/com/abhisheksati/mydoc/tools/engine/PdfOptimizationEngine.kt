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

        // Strategy C: Pure object and xref compaction via MuPDF
        val cleanOutFile = createTempOutputFile("compressed_clean", "pdf")
        val cleanParams = when (options.compressionLevel) {
            // "deflate" is not a MuPDF writer option; the save threw and BALANCED and STRONG
            // silently fell through to another strategy while claiming to compact the file.
            // "compress" already deflates the streams it writes.
            CompressionLevel.LIGHT -> "compress,clean,garbage=2"
            CompressionLevel.BALANCED -> "compress,clean,garbage=3"
            CompressionLevel.STRONG -> "compress,clean,garbage=4"
        }
        val srcDoc = try {
            Document.openDocument(file.absolutePath) as? PDFDocument
        } catch (_: Throwable) {
            null
        }
        var cleanSucceeded = false
        if (srcDoc != null) {
            try {
                srcDoc.save(cleanOutFile.absolutePath, cleanParams)
                cleanSucceeded = true
            } catch (_: Exception) {
                cleanOutFile.delete()
            }
        }

        // Strategy B: Only as an extreme fallback for STRONG compression when vector-preserving
        // methods cannot reduce size, to avoid destroying text searchability and vector quality.
        val rasterOutFile = createTempOutputFile("compressed_raster", "pdf")
        var rasterSucceeded = false
        val nonDestructiveReduced = (pdfBoxSucceeded && boxOutFile.exists() && boxOutFile.length() < originalSize * 0.95f) ||
                (cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() < originalSize * 0.95f)

        if (srcDoc != null && options.compressionLevel == CompressionLevel.STRONG && !nonDestructiveReduced) {
            try {
                val totalPages = srcDoc.countPages()
                val (renderScale, jpegQuality) = Pair(0.85f, 45)

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
        }
        srcDoc?.destroy()

        onProgress(ToolProgress(100, 100, 95, "Selecting most efficient compression...", false))

        // Choose the best candidate that preserves text/vector, or raster if strong compression succeeded
        val nonDestructiveCandidates = mutableListOf<File>()
        if (pdfBoxSucceeded && boxOutFile.exists() && boxOutFile.length() > 0 && boxOutFile.length() < originalSize) {
            nonDestructiveCandidates.add(boxOutFile)
        }
        if (cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() < originalSize) {
            nonDestructiveCandidates.add(cleanOutFile)
        }

        val bestNonDestructive = nonDestructiveCandidates.minByOrNull { it.length() }
        if (bestNonDestructive != null) {
            if (bestNonDestructive != boxOutFile && boxOutFile.exists()) boxOutFile.delete()
            if (bestNonDestructive != cleanOutFile && cleanOutFile.exists()) cleanOutFile.delete()
            if (rasterOutFile.exists()) rasterOutFile.delete()
            return bestNonDestructive
        }

        if (rasterSucceeded && rasterOutFile.exists() && rasterOutFile.length() > 0 && rasterOutFile.length() < originalSize * 0.85f) {
            if (boxOutFile.exists()) boxOutFile.delete()
            if (cleanOutFile.exists()) cleanOutFile.delete()
            return rasterOutFile
        }

        if (cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() <= originalSize) {
            if (boxOutFile.exists()) boxOutFile.delete()
            if (rasterOutFile.exists()) rasterOutFile.delete()
            return cleanOutFile
        }

        // Guarantee a standalone new temp file is returned so caller's finally cleanup never deletes the result
        val fallbackOut = createTempOutputFile("compressed_fallback", "pdf")
        if (boxOutFile.exists() && boxOutFile.length() > 0) {
            boxOutFile.copyTo(fallbackOut, overwrite = true)
            boxOutFile.delete()
        } else {
            file.copyTo(fallbackOut, overwrite = true)
        }
        if (cleanOutFile.exists()) cleanOutFile.delete()
        if (rasterOutFile.exists()) rasterOutFile.delete()
        return fallbackOut
    }

    suspend fun repairPdf(file: File, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = try {
            Document.openDocument(file.absolutePath) as? PDFDocument
        } catch (_: Throwable) {
            null
        }
        if (srcDoc != null) {
            try {
                val totalPages = srcDoc.countPages()
                val destDoc = PDFDocument()
                var recoveredPages = 0

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    try {
                        destDoc.graftPage(destDoc.countPages(), srcDoc, p)
                        recoveredPages++
                    } catch (_: Exception) {
                        // Skip corrupt page gracefully
                    }
                    val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Recovering page ${p + 1}...", false))
                }

                if (recoveredPages == 0 || destDoc.countPages() == 0) {
                    destDoc.destroy()
                    throw IllegalStateException("Document repair failed: No readable pages could be recovered from this damaged file.")
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
            try {
            FileInputStream(file).use { fis ->
                PDDocument.load(fis).use { doc ->
                    if (doc.numberOfPages == 0) {
                        throw IllegalStateException("Document repair failed: Recovered document has 0 pages.")
                    }
                    FileOutputStream(outFile).use { fos -> doc.save(fos) }
                }
            }
            } catch (e: Exception) {
                if (e is IllegalStateException) throw e
                throw IllegalStateException("Document repair failed: Corrupted PDF cannot be parsed or recovered.", e)
            }
            return outFile
        }
    }

}
