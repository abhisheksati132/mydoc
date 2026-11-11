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
import kotlinx.coroutines.CancellationException
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

    private companion object {
        /** An embedded image larger than this (~24 MP, ~96 MB decoded) is left alone rather than
         *  decoded, because the decode itself is what exhausts memory on an image-heavy scan. */
        const val MAX_DECODABLE_PIXELS = 24_000_000L
    }

    private fun createTempOutputFile(prefix: String, ext: String): File =
        PdfToolUtils.createTempOutputFile(context.cacheDir, prefix, ext)

    suspend fun compressPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val originalSize = file.length()
        onProgress(ToolProgress(0, 100, 10, "Analyzing document streams and images...", false))

        // Strategy A: Recompress embedded image streams with PDFBox
        var totalImagesRecompressed = 0
        var totalImagesSkipped = 0
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
                            if (!resources.isImageXObject(name)) continue
                            val xobj = resources.getXObject(name) as? PDImageXObject ?: continue
                            val w = xobj.width
                            val h = xobj.height

                            // The stream dictionary already gives the dimensions, so an image too large
                            // to hold is skipped before it is decoded rather than after it dies.
                            if (w.toLong() * h.toLong() > MAX_DECODABLE_PIXELS) {
                                android.util.Log.w(
                                    "PdfOptimize",
                                    "skipping a ${w}x$h image: too large to decode within the budget"
                                )
                                totalImagesSkipped++
                                continue
                            }

                            val origBmp = try {
                                xobj.image
                            } catch (e: CancellationException) {
                                throw e
                            } catch (t: Throwable) {
                                android.util.Log.w("PdfOptimize", "could not decode a ${w}x$h image", t)
                                totalImagesSkipped++
                                continue
                            }

                            try {
                                val scale = minOf(1.0f, maxDim.toFloat() / maxOf(w, h).toFloat())
                                val targetW = (w * scale).toInt().coerceAtLeast(1)
                                val targetH = (h * scale).toInt().coerceAtLeast(1)

                                val scaledBmp = if (targetW != w || targetH != h) {
                                    Bitmap.createScaledBitmap(origBmp, targetW, targetH, true)
                                } else {
                                    origBmp
                                }

                                try {
                                    resources.put(
                                        name,
                                        JPEGFactory.createFromImage(doc, scaledBmp, qualityFloat)
                                    )
                                    totalImagesRecompressed++
                                } finally {
                                    if (scaledBmp !== origBmp) {
                                        scaledBmp.recycle()
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (t: Throwable) {
                                android.util.Log.w("PdfOptimize", "could not recompress a ${w}x$h image", t)
                                totalImagesSkipped++
                            } finally {
                                // PDImageXObject.getImage() decodes afresh on every call, so this bitmap
                                // belongs to this loop; the encoded JPEG no longer refers to it.
                                origBmp.recycle()
                            }
                        }
                        val pct = 10 + ((p.toFloat() / totalPages) * 35).toInt()
                        onProgress(ToolProgress(p + 1, totalPages, pct, "Optimizing image streams...", false))
                    }

                    if (totalImagesSkipped > 0) {
                        android.util.Log.w(
                            "PdfOptimize",
                            "left $totalImagesSkipped embedded image(s) untouched: too large or undecodable"
                        )
                    }

                    if (totalImagesRecompressed > 0) {
                        FileOutputStream(boxOutFile).use { fos -> doc.save(fos) }
                        pdfBoxSucceeded = true
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("PdfOptimize", "image recompression strategy failed", e)
            pdfBoxSucceeded = false
            if (boxOutFile.exists()) boxOutFile.delete()
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
                cleanSucceeded = cleanOutFile.exists() && cleanOutFile.length() > 0 && isValidPdf(cleanOutFile)
            } catch (_: Exception) {
                cleanSucceeded = false
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
            val outDoc = AndroidPdfDocument()
            try {
                val totalPages = srcDoc.countPages()
                val renderScale = 0.85f

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

                    val pageInfo = AndroidPdfDocument.PageInfo.Builder(w, h, p + 1).create()
                    val aPage = outDoc.startPage(pageInfo)
                    try {
                        aPage.canvas.drawBitmap(bmp, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), null)
                    } finally {
                        outDoc.finishPage(aPage)
                        bmp.recycle()
                    }

                    val pct = 45 + ((p.toFloat() / totalPages) * 45).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Compressing page ${p + 1} of $totalPages...", false))
                }

                FileOutputStream(rasterOutFile).use { outDoc.writeTo(it) }
                rasterSucceeded = rasterOutFile.exists() && rasterOutFile.length() > 0 && isValidPdf(rasterOutFile)
            } catch (_: Exception) {
                rasterSucceeded = false
                rasterOutFile.delete()
            } finally {
                outDoc.close()
            }
        }
        srcDoc?.destroy()

        onProgress(ToolProgress(100, 100, 95, "Selecting most efficient compression...", false))

        // Choose the best candidate that preserves text/vector, or raster if strong compression succeeded
        val nonDestructiveCandidates = mutableListOf<File>()
        if (pdfBoxSucceeded && boxOutFile.exists() && boxOutFile.length() > 0 && boxOutFile.length() < originalSize && isValidPdf(boxOutFile)) {
            nonDestructiveCandidates.add(boxOutFile)
        } else if (boxOutFile.exists()) {
            boxOutFile.delete()
        }

        if (cleanSucceeded && cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() < originalSize && isValidPdf(cleanOutFile)) {
            nonDestructiveCandidates.add(cleanOutFile)
        } else if (cleanOutFile.exists()) {
            cleanOutFile.delete()
        }

        val bestNonDestructive = nonDestructiveCandidates.minByOrNull { it.length() }
        if (bestNonDestructive != null) {
            if (bestNonDestructive != boxOutFile && boxOutFile.exists()) boxOutFile.delete()
            if (bestNonDestructive != cleanOutFile && cleanOutFile.exists()) cleanOutFile.delete()
            if (rasterOutFile.exists()) rasterOutFile.delete()
            return bestNonDestructive
        }

        if (rasterSucceeded && rasterOutFile.exists() && rasterOutFile.length() > 0 && rasterOutFile.length() < originalSize * 0.85f && isValidPdf(rasterOutFile)) {
            if (boxOutFile.exists()) boxOutFile.delete()
            if (cleanOutFile.exists()) cleanOutFile.delete()
            return rasterOutFile
        } else if (rasterOutFile.exists()) {
            rasterOutFile.delete()
        }

        if (cleanSucceeded && cleanOutFile.exists() && cleanOutFile.length() > 0 && cleanOutFile.length() <= originalSize && isValidPdf(cleanOutFile)) {
            if (boxOutFile.exists()) boxOutFile.delete()
            if (rasterOutFile.exists()) rasterOutFile.delete()
            return cleanOutFile
        }

        // Guarantee a standalone new temp file is returned so caller's finally cleanup never deletes the result
        val fallbackOut = createTempOutputFile("compressed_fallback", "pdf")
        if (pdfBoxSucceeded && boxOutFile.exists() && isValidPdf(boxOutFile)) {
            boxOutFile.copyTo(fallbackOut, overwrite = true)
            boxOutFile.delete()
        } else {
            file.copyTo(fallbackOut, overwrite = true)
        }
        if (boxOutFile.exists()) boxOutFile.delete()
        if (cleanOutFile.exists()) cleanOutFile.delete()
        if (rasterOutFile.exists()) rasterOutFile.delete()
        return fallbackOut
    }

    private fun isValidPdf(file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        return try {
            val doc = Document.openDocument(file.absolutePath)
            val pages = doc.countPages()
            doc.destroy()
            pages > 0
        } catch (_: Throwable) {
            false
        }
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
