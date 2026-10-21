package com.abhisheksati.mydoc.domain.model

enum class DocumentCategory(val label: String) {
    ALL("All"),
    PDF("PDF"),
    WORD("Word"),
    EXCEL("Excel"),
    PPT("PowerPoint"),
    TEXT("Text");

    companion object {
        val FAVORITES = ALL
        val EPUB = TEXT
    }
}
