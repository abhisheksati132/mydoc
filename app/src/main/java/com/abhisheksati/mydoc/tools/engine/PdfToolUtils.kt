package com.abhisheksati.mydoc.tools.engine

object PdfToolUtils {

    /**
     * Parse human page range strings (e.g. "1, 3-5, 8") into 0-indexed page numbers.
     * Safely handles out-of-bounds, inverted ranges ("5-2"), negative values, and non-numeric characters.
     */
    fun parsePageRanges(rangeStr: String, maxPages: Int): List<Int> {
        if (maxPages <= 0) return emptyList()
        if (rangeStr.isBlank()) return (0 until maxPages).toList()
        val result = mutableSetOf<Int>()
        val parts = rangeStr.split(",")
        for (part in parts) {
            val clean = part.trim()
            if (clean.contains("-")) {
                val bounds = clean.split("-")
                val rawStart = bounds.getOrNull(0)?.trim()?.toIntOrNull()
                val rawEnd = bounds.getOrNull(1)?.trim()?.toIntOrNull()

                // If neither bound was a valid number (e.g. lone "-" or "- -"), skip
                if (rawStart == null && rawEnd == null) {
                    continue
                }

                var start = (rawStart ?: 1) - 1
                var end = (rawEnd ?: maxPages) - 1

                // If entire requested range lies strictly outside the document, skip
                if (minOf(start, end) >= maxPages || maxOf(start, end) < 0) {
                    continue
                }

                if (start > end) {
                    val temp = start
                    start = end
                    end = temp
                }

                val clampedStart = start.coerceIn(0, maxPages - 1)
                val clampedEnd = end.coerceIn(0, maxPages - 1)
                for (i in clampedStart..clampedEnd) {
                    result.add(i)
                }
            } else {
                val num = clean.toIntOrNull()
                if (num != null && num in 1..maxPages) {
                    result.add(num - 1)
                }
            }
        }
        return result.sorted()
    }

    /**
     * Sanitize user or document text for safe XML embedding by stripping illegal characters
     * and escaping XML special entities.
     */
    fun sanitizeXml(text: String): String {
        return text.filter { ch ->
            ch == '\t' || ch == '\n' || ch == '\r' || (ch.code in 0x20..0xD7FF) || (ch.code in 0xE000..0xFFFD)
        }.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Create a managed temporary output file in the tools output cache directory.
     */
    fun createTempOutputFile(cacheDir: java.io.File, prefix: String, ext: String): java.io.File {
        val toolsDir = java.io.File(cacheDir, "tools_output").apply { if (!exists()) mkdirs() }
        return java.io.File.createTempFile("mydoc_${prefix}_", ".$ext", toolsDir)
    }
}
