package com.abhisheksati.mydoc.rendering.engine

import java.io.File

/**
 * High-performance sidecar cache for non-PDF documents (PowerPoint, Excel)
 * that converts outlines/table of contents to instant index markers.
 */
object OutlineCache {

    fun saveOutlines(pdfFile: File, outlines: List<OutlineItem>) {
        if (outlines.isEmpty()) return
        runCatching {
            val outlineFile = File(pdfFile.parentFile, "${pdfFile.name}.outline")
            outlineFile.printWriter().use { writer ->
                for (item in outlines) {
                    writer.println("${item.pageIndex}|${item.level}|${item.title.replace('|', '/')}")
                }
            }
        }
    }

    fun loadOutlines(pdfPath: String): List<OutlineItem> {
        return runCatching {
            val outlineFile = File("${pdfPath}.outline")
            if (!outlineFile.exists() || outlineFile.length() == 0L) return emptyList()
            outlineFile.readLines().mapNotNull { line ->
                val parts = line.split('|', limit = 3)
                if (parts.size == 3) {
                    val pageIdx = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val level = parts[1].toIntOrNull() ?: 0
                    val title = parts[2]
                    OutlineItem(title = title, pageIndex = pageIdx, level = level)
                } else null
            }
        }.getOrDefault(emptyList())
    }
}
