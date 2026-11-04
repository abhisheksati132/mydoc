package com.abhisheksati.mydoc.domain.model

import java.util.Locale

/**
 * The one document-filter vocabulary used by Home and Files, so a label never
 * means two different sets of extensions depending on which tab you are on.
 *
 * Each filter is a [DocumentFamily] view: the membership comes from [DocumentFormats], so a
 * format added or retired in the registry appears or disappears here without a second edit.
 */
enum class DocumentFilter(val label: String, val family: DocumentFamily) {
    ALL("All", DocumentFamily.UNKNOWN),
    PDF("PDF", DocumentFamily.PDF),
    WORD("Word", DocumentFamily.WORD),
    EXCEL("Excel", DocumentFamily.SPREADSHEET),
    PPT("PPT", DocumentFamily.PRESENTATION),
    TEXT("Text", DocumentFamily.TEXT);

    fun matches(extension: String): Boolean {
        if (this == ALL) return true
        val ext = extension.lowercase(Locale.ROOT)
        return ext in DocumentFormats.extensionsIn(family)
    }

    companion object {
        /** Formats exposed as chips; [ALL] is the implicit default. */
        val formats: List<DocumentFilter> = entries.filter { it != ALL }

        /** Case-insensitive lookup for callers that only have a chip label. */
        fun fromLabel(label: String?): DocumentFilter? =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
}
