package com.abhisheksati.mydoc.tools.engine

import android.content.Context
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import com.abhisheksati.mydoc.tools.pdf.internal.PdfBoxHelper
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFDocument
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Specialized engine handling PDF page manipulations: merge, split, reorder, extract, remove, rotate, and crop.
 */
class PdfPageOperationsEngine(private val context: Context) {

    suspend fun mergePdfs(files: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val destDoc = PDFDocument()
        val totalFiles = files.size

        files.forEachIndexed { fileIndex, file ->
            currentCoroutineContext().ensureActive()
            val progressPercent = 20 + ((fileIndex.toFloat() / totalFiles) * 70).toInt()
            onProgress(ToolProgress(fileIndex + 1, totalFiles, progressPercent, "Merging document ${fileIndex + 1} of $totalFiles...", false))

            val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
                ?: throw IllegalArgumentException("Cannot open ${file.name} as a valid PDF")
            try {
                val pageCount = srcDoc.countPages()
                val selectedIndices = PdfToolUtils.parsePageRanges(options.pageRange, pageCount)
                for (p in selectedIndices) {
                    currentCoroutineContext().ensureActive()
                    destDoc.graftPage(destDoc.countPages(), srcDoc, p)
                }
            } finally {
                srcDoc.destroy()
            }
        }

        val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "merged", "pdf")
        destDoc.save(outFile.absolutePath, "compress,garbage=3")
        destDoc.destroy()
        return outFile
    }

    suspend fun splitPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val range = options.pageRange.trim()

            // A typed range means "keep these pages in one PDF"; otherwise split every N pages.
            if (range.isNotEmpty()) {
                val indices = PdfToolUtils.parsePageRanges(range, totalPages)
                if (indices.isEmpty()) {
                    throw IllegalArgumentException("No pages matched the range \"$range\".")
                }
                val outDoc = PDFDocument()
                for (p in indices) {
                    currentCoroutineContext().ensureActive()
                    outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                }
                onProgress(ToolProgress(indices.size, indices.size, 85, "Saving selected pages...", false))
                val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "split", "pdf")
                outDoc.save(outFile.absolutePath, "compress")
                outDoc.destroy()
                return outFile
            }

            val step = options.splitEveryNPages.coerceAtLeast(1)

            // If split results in multiple parts, package into ZIP
            val partsCount = (totalPages + step - 1) / step
            if (partsCount > 1) {
                val zipFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "split_parts", "zip")
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    var partNum = 1
                    var pageIndex = 0
                    while (pageIndex < totalPages) {
                        currentCoroutineContext().ensureActive()
                        val subDoc = PDFDocument()
                        val endIndex = (pageIndex + step).coerceAtMost(totalPages)
                        for (p in pageIndex until endIndex) {
                            subDoc.graftPage(subDoc.countPages(), srcDoc, p)
                        }

                        val partFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "part_${partNum}", "pdf")
                        subDoc.save(partFile.absolutePath, "compress")
                        subDoc.destroy()

                        // Add to zip
                        zos.putNextEntry(ZipEntry(String.format(java.util.Locale.ROOT, "part_%03d.pdf", partNum)))
                        partFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                        partFile.delete()

                        partNum++
                        pageIndex = endIndex
                        val pct = 20 + ((pageIndex.toFloat() / totalPages) * 75).toInt()
                        onProgress(ToolProgress(pageIndex, totalPages, pct, "Splitting pages $pageIndex of $totalPages...", false))
                    }
                }
                return zipFile
            } else {
                // Single part: every page in one file
                val outDoc = PDFDocument()
                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                }
                val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "split", "pdf")
                outDoc.save(outFile.absolutePath, "compress")
                outDoc.destroy()
                return outFile
            }
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun removePages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val toRemove = PdfToolUtils.parsePageRanges(options.pageRange, totalPages).toSet()
            if (toRemove.size >= totalPages) {
                throw IllegalArgumentException("Cannot remove all pages from document.")
            }

            val outDoc = PDFDocument()
            for (p in 0 until totalPages) {
                currentCoroutineContext().ensureActive()
                if (p !in toRemove) {
                    outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                }
            }
            onProgress(ToolProgress(1, 1, 85, "Saving pruned PDF...", false))
            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "pruned", "pdf")
            outDoc.save(outFile.absolutePath, "compress,clean,garbage=3")
            outDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun reorderPages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val order = if (options.reorderedPageIndices.isNotEmpty()) {
                options.reorderedPageIndices.filter { it in 0 until totalPages }
            } else {
                (0 until totalPages).toList()
            }

            val outDoc = PDFDocument()
            order.forEachIndexed { idx, p ->
                currentCoroutineContext().ensureActive()
                outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                val pct = 20 + ((idx.toFloat() / order.size) * 70).toInt()
                onProgress(ToolProgress(idx + 1, order.size, pct, "Reordering page ${idx + 1}...", false))
            }

            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "reordered", "pdf")
            outDoc.save(outFile.absolutePath, "compress,garbage=2")
            outDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun extractPages(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        val srcDoc = Document.openDocument(file.absolutePath) as? PDFDocument
            ?: throw IllegalArgumentException("Cannot open document")
        try {
            val totalPages = srcDoc.countPages()
            val indices = PdfToolUtils.parsePageRanges(options.pageRange, totalPages)
            if (indices.isEmpty()) {
                throw IllegalArgumentException("No valid pages specified for extraction.")
            }

            val outDoc = PDFDocument()
            indices.forEachIndexed { i, p ->
                currentCoroutineContext().ensureActive()
                outDoc.graftPage(outDoc.countPages(), srcDoc, p)
                val pct = 20 + ((i.toFloat() / indices.size) * 70).toInt()
                onProgress(ToolProgress(i + 1, indices.size, pct, "Extracting page ${i + 1}...", false))
            }

            val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "extracted", "pdf")
            outDoc.save(outFile.absolutePath, "compress")
            outDoc.destroy()
            return outFile
        } finally {
            srcDoc.destroy()
        }
    }

    suspend fun rotatePdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        PdfBoxHelper.ensureInitialized(context)
        val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "rotated", "pdf")
        val degrees = options.rotateDegrees

        FileInputStream(file).use { input ->
            PDDocument.load(input).use { doc ->
                val totalPages = doc.numberOfPages
                val indices = if (options.pageRange.isNotBlank()) {
                    PdfToolUtils.parsePageRanges(options.pageRange, totalPages).toSet()
                } else {
                    (0 until totalPages).toSet()
                }

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    if (p in indices) {
                        val page = doc.getPage(p)
                        page.rotation = (page.rotation + degrees) % 360
                    }
                    val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Rotating page ${p + 1} of $totalPages...", false))
                }

                FileOutputStream(outFile).use { out -> doc.save(out) }
            }
        }
        return outFile
    }

    suspend fun cropPdf(file: File, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File {
        PdfBoxHelper.ensureInitialized(context)
        val outFile = PdfToolUtils.createTempOutputFile(context.cacheDir, "cropped", "pdf")
        val marginFrac = options.cropMarginPercent.coerceIn(0.01f, 0.45f)

        FileInputStream(file).use { input ->
            PDDocument.load(input).use { doc ->
                val totalPages = doc.numberOfPages
                val indices = if (options.pageRange.isNotBlank()) {
                    PdfToolUtils.parsePageRanges(options.pageRange, totalPages).toSet()
                } else {
                    (0 until totalPages).toSet()
                }

                for (p in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()
                    if (p in indices) {
                        val page = doc.getPage(p)
                        val box = page.cropBox ?: page.mediaBox ?: continue
                        val insetX = box.width * marginFrac
                        val insetY = box.height * marginFrac
                        val newCrop = PDRectangle(
                            box.lowerLeftX + insetX,
                            box.lowerLeftY + insetY,
                            box.width - (2f * insetX),
                            box.height - (2f * insetY)
                        )
                        page.cropBox = newCrop
                    }
                    val pct = 20 + ((p.toFloat() / totalPages) * 70).toInt()
                    onProgress(ToolProgress(p + 1, totalPages, pct, "Cropping page ${p + 1} of $totalPages...", false))
                }

                FileOutputStream(outFile).use { out -> doc.save(out) }
            }
        }
        return outFile
    }
}
