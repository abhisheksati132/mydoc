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
                if (bounds.size != 2) {
                    continue
                }
                val startStr = bounds[0].trim()
                val endStr = bounds[1].trim()

                // If neither bound was a valid number (e.g. lone "-" or "- -"), skip
                if (startStr.isEmpty() && endStr.isEmpty()) {
                    continue
                }

                // If non-empty but not a valid number (e.g. "abc-3"), skip
                val rawStart = if (startStr.isEmpty()) 1 else startStr.toIntOrNull() ?: continue
                val rawEnd = if (endStr.isEmpty()) maxPages else endStr.toIntOrNull() ?: continue

                var start = rawStart - 1
                var end = rawEnd - 1

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
     *
     * Walks code points, not chars: a non-BMP character is a surrogate pair, and judging each half
     * against [#x20-#xD7FF] would delete every emoji and rare CJK character from the output. Legal
     * XML 1.0 characters are #x9, #xA, #xD, [#x20-#xD7FF], [#xE000-#xFFFD] and [#x10000-#x10FFFF].
     */
    fun sanitizeXml(text: String): String {
        val filtered = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (isLegalXmlCodePoint(codePoint)) filtered.appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
        }
        return filtered.toString().replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    internal fun isLegalXmlCodePoint(codePoint: Int): Boolean =
        codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD ||
            (codePoint in 0x20..0xD7FF) ||
            (codePoint in 0xE000..0xFFFD) ||
            (codePoint in 0x10000..0x10FFFF)

    /**
     * Create a managed temporary output file in the tools output cache directory.
     */
    fun createTempOutputFile(cacheDir: java.io.File, prefix: String, ext: String): java.io.File {
        val toolsDir = java.io.File(cacheDir, "tools_output").apply { if (!exists()) mkdirs() }
        return java.io.File.createTempFile("mydoc_${prefix}_", ".$ext", toolsDir)
    }
}
