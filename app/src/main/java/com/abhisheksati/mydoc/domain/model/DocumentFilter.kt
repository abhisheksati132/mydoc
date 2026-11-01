package com.abhisheksati.mydoc.domain.model

import java.util.Locale

/**
 * The one document-filter vocabulary used by Home and Files, so a label never
 * means two different sets of extensions depending on which tab you are on.
 */
enum class DocumentFilter(val label: String) {
    ALL("All"),
    PDF("PDF"),
    WORD("Word"),
    EXCEL("Excel"),
    PPT("PPT"),
    TEXT("Text");

    fun matches(extension: String): Boolean {
        val ext = extension.lowercase(Locale.ROOT)
        return when (this) {
            ALL -> true
            PDF -> ext == "pdf"
            WORD -> ext in WORD_EXTENSIONS
            EXCEL -> ext in EXCEL_EXTENSIONS
            PPT -> ext in PPT_EXTENSIONS
            TEXT -> ext in TEXT_EXTENSIONS
        }
    }

    companion object {
        /** Formats exposed as chips; [ALL] is the implicit default. */
        val formats: List<DocumentFilter> = entries.filter { it != ALL }

        private val WORD_EXTENSIONS = setOf("doc", "docx", "dot", "dotx", "odt", "rtf")
        private val EXCEL_EXTENSIONS = setOf("xls", "xlsx", "xlsm", "csv", "ods")
        private val PPT_EXTENSIONS = setOf("ppt", "pptx", "pps", "odp")
        private val TEXT_EXTENSIONS = setOf("txt", "log", "md", "json", "xml")
    }
}
