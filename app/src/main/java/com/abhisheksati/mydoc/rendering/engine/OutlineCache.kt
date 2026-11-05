package com.abhisheksati.mydoc.rendering.engine

import java.io.File

/**
 * Sidecar cache for the outline of a converted non-PDF document (PowerPoint, Excel, Word), so the
 * table of contents is available as instant index markers without re-parsing the source.
 *
 * The sidecar records the size and modification time of the PDF it was written from, because a
 * sidecar keyed only by filename would keep serving yesterday's outline to a document that has been
 * converted again under the same name. An empty outline clears the sidecar rather than leaving the
 * previous one in place.
 */
object OutlineCache {

    private const val SIDECAR_SUFFIX = ".outline"
    private const val STAMP_PREFIX = "#outline-stamp:"
    private const val FIELD = '\t'

    fun sidecarFileFor(pdfFile: File): File = File(pdfFile.parentFile, pdfFile.name + SIDECAR_SUFFIX)

    fun saveOutlines(pdfFile: File, outlines: List<OutlineItem>) {
        val outlineFile = sidecarFileFor(pdfFile)
        if (outlines.isEmpty()) {
            outlineFile.delete()
            return
        }
        runCatching {
            outlineFile.printWriter().use { writer ->
                writer.println("$STAMP_PREFIX${pdfFile.length()}$FIELD${pdfFile.lastModified()}")
                for (item in outlines) {
                    writer.println(
                        "${item.pageIndex}$FIELD${item.level}$FIELD${escape(item.title)}"
                    )
                }
            }
        }
    }

    fun loadOutlines(pdfPath: String): List<OutlineItem> = loadOutlines(File(pdfPath))

    fun loadOutlines(pdfFile: File): List<OutlineItem> {
        val outlineFile = sidecarFileFor(pdfFile)
        if (!outlineFile.isFile || outlineFile.length() == 0L) return emptyList()

        return runCatching {
            val lines = outlineFile.readLines()
            val stamp = lines.firstOrNull().orEmpty()
            if (!stamp.startsWith(STAMP_PREFIX)) {
                // Written by an older build, or not a sidecar at all: distrust it.
                outlineFile.delete()
                return@runCatching emptyList()
            }
            val fields = stamp.removePrefix(STAMP_PREFIX).split(FIELD)
            val recordedSize = fields.getOrNull(0)?.toLongOrNull()
            val recordedModified = fields.getOrNull(1)?.toLongOrNull()

            if (pdfFile.isFile &&
                (recordedSize == null || recordedModified == null ||
                    pdfFile.length() != recordedSize || pdfFile.lastModified() != recordedModified)
            ) {
                outlineFile.delete()
                return@runCatching emptyList()
            }

            lines.drop(1).mapNotNull { line ->
                val parts = line.split(FIELD, limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val pageIndex = parts[0].toIntOrNull() ?: return@mapNotNull null
                val level = parts[1].toIntOrNull() ?: 0
                OutlineItem(title = unescape(parts[2]), pageIndex = pageIndex, level = level)
            }
        }.getOrDefault(emptyList())
    }

    /** Tab-separated, so a title containing a pipe no longer has to be mangled to survive. */
    private fun escape(title: String): String = title
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\r", " ")
        .replace("\n", " ")

    private fun unescape(title: String): String = StringBuilder(title.length).let { out ->
        var index = 0
        while (index < title.length) {
            val char = title[index]
            if (char == '\\' && index + 1 < title.length) {
                val next = title[index + 1]
                when (next) {
                    't' -> out.append('\t')
                    '\\' -> out.append('\\')
                    else -> out.append(next)
                }
                index += 2
            } else {
                out.append(char)
                index++
            }
        }
        out.toString()
    }
}
