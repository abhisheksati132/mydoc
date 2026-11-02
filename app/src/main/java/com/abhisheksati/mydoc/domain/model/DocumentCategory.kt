package com.abhisheksati.mydoc.domain.model

enum class DocumentCategory(val label: String) {
    ALL("All"),
    PDF("PDF"),
    WORD("Word"),
    EXCEL("Excel"),
    PPT("PowerPoint"),
    TEXT("Text"),
    EPUB("eBook");

    companion object {
        val FAVORITES = ALL
    }
}
